package com.asterion.art.video

data class WanNeuralSegment(
    val index:Int,
    val sourceIndex:Int,
    val usePreviousEndFrame:Boolean,
    val expectedSeconds:Int,
    val seed:Long
)

/**
 * Native Wan TI2V 5B generation is divided into one-second 25-frame windows,
 * preserving genuine 24fps without allocating 121 full decoded frames at once.
 * Every next window conditions on its predecessor's ACTUAL final generated frame.
 */
object WanNeuralTimeline {
    fun plan(sourceImages:Int,secondsPerAnchor:Int,numberOfAnchors:Int,
        seed:Long):List<WanNeuralSegment> {
        require(sourceImages in 1..8)
        require(secondsPerAnchor in 1..5)
        require(numberOfAnchors in 1..24)
        require(sourceImages==1 || numberOfAnchors==sourceImages)
        return buildList {
            repeat(numberOfAnchors){anchor->
                repeat(secondsPerAnchor){second->
                    val n=size
                    add(WanNeuralSegment(
                        index=n,
                        sourceIndex=if(sourceImages==1)0 else anchor,
                        usePreviousEndFrame=second>0||
                            (sourceImages==1 && anchor>0),
                        expectedSeconds=1,
                        seed=seed+n*1009L
                    ))
                }
            }
        }
    }
}
