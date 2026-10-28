package com.asterion.art.learning

import kotlin.math.abs
import kotlin.math.max

/**
 * Held-out, per-creation-mode predictive validation. The local regression
 * model may not claim improvement merely because training loss decreases.
 * Validation uses DISTINCT user-assessed images and a temporal 80/20 split.
 */
data class VisualLearningMetric(
    val mode:String,val dimension:String,val trainingImages:Int,
    val heldOutImages:Int,val baselineMae:Double?,val modelMae:Double?,
    val validated:Boolean
) {
    val improvement:Double? get()=if(baselineMae!=null&&modelMae!=null)
        baselineMae-modelMae else null
}

object VisualLearningValidation {
    private data class Sample(
        val asset:String,val updated:Long,val value:Int,
        val features:List<Double>,val grade:DetailedGrade
    )
    fun evaluate(grades:List<DetailedGrade>):List<VisualLearningMetric> {
        val valid=grades.filter { grade ->
            grade.visualFeatures.size==VisualGradeFeatures.SIZE &&
                grade.visualFeatures.all{it.isFinite()&&it in 0.0..1.0}
        }
        val entries=valid.flatMap { grade ->
            val mode=grade.creationMode?.name?:grade.renderMode?.name?:"UNSCOPED"
            val axes=grade.ratings.map{it.key.name to it.value}+
                listOfNotNull(grade.overall?.let{"OVERALL" to it})
            axes.map{(axis,score)->
                (mode to axis) to Sample(grade.assetUri,grade.updatedAt,score,
                    grade.visualFeatures,grade)
            }
        }
        return entries.groupBy{it.first}.map { (key,items) ->
            // Multiple grades of one source must not leak the same pixels
            // across the holdout boundary or inflate the sample count.
            val unique=items.map{it.second}
                .groupBy{it.asset}
                .values.map{records->records.maxBy{it.updated}}
                .sortedWith(compareBy<Sample>({it.updated},{it.asset}))
            val n=unique.size
            if(n<20) {
                VisualLearningMetric(key.first,key.second,n,0,null,null,false)
            } else {
                val split=(n*0.8).toInt().coerceIn(12,n-4)
                val training=unique.take(split)
                val heldOut=unique.drop(split)
                val model=GradingVisualTraining.train(training.map{it.grade})
                    .firstOrNull{it.mode==key.first&&it.dimension==key.second}
                val mean=training.map{it.value.toDouble()}.average()
                val baseline=heldOut.map{abs(it.value-mean)}.average()
                val prediction=model?.let{predictor->
                    heldOut.map{abs(it.value-predictor.predict(it.features))}
                        .average()
                }
                VisualLearningMetric(key.first,key.second,training.size,
                    heldOut.size,baseline,prediction,
                    prediction!=null && baseline>0.05 &&
                        prediction<=baseline*0.95 &&
                        baseline-prediction>=0.03)
            }
        }.sortedWith(compareBy({it.mode},{it.dimension}))
    }
}
