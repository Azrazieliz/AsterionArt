package com.asterion.art.model

/**
 * Shared presentation geometry based on the clean Character Reference Sheet:
 * one unmistakable primary visual, rigorously aligned supporting evidence,
 * calm whitespace, panel titles, and a separate measured color area.
 *
 * This is only layout. Every sheet type retains its own semantic panel schema,
 * source images, visual authority, and generation methodology.
 */
data class SheetBounds(
    val left:Int,val top:Int,val right:Int,val bottom:Int
) {
    init { require(right>left && bottom>top) }
    val width:Int get()=right-left
    val height:Int get()=bottom-top
    fun overlaps(other:SheetBounds):Boolean =
        left<other.right && right>other.left &&
            top<other.bottom && bottom>other.top
}

data class SheetPanelPlacement(
    val sourceIndex:Int,val slot:String,val frame:SheetBounds,
    val principal:Boolean
)

data class SheetPresentationPlan(
    val title:String,
    val referenceCount:Int,
    val placements:List<SheetPanelPlacement>,
    val swatchArea:SheetBounds,
    val pageWidth:Int,
    val pageHeight:Int
)

/** Pixel-exact 9:16 template, independent of which typed references were provided. */
object SheetPresentation {
    const val WIDTH=1440
    const val HEIGHT=2560
    const val MAX_REFS=10

    fun title(role:SheetRole):String=when(role) {
        SheetRole.CHARACTER -> "CHARACTER REFERENCE SHEET"
        SheetRole.ART_STYLE -> "ART STYLE REFERENCE SHEET"
        SheetRole.THEME -> "THEME REFERENCE SHEET"
        SheetRole.ELEMENT -> "ELEMENT REFERENCE SHEET"
    }

    fun plan(role:SheetRole,assignedSlots:List<String>):SheetPresentationPlan {
        require(assignedSlots.size in 1..MAX_REFS)
        require(assignedSlots.any{it=="PRIMARY"}){"A primary reference is required"}
        require(assignedSlots.all{SheetLayouts.isValid(role,it)}) {
            "A source panel is not valid for this reference-sheet type"
        }
        val priorities=SheetLayouts.forRole(role).associate{it.id to it.priority}
        val ordered=assignedSlots.indices.sortedWith(
            compareBy<Int> {priorities.getValue(assignedSlots[it])}.thenBy{it})
        val bodyTop=198
        val bodyBottom=2266
        val gap=16
        val outerLeft=34
        val outerRight=WIDTH-34
        val placements=mutableListOf<SheetPanelPlacement>()
        val primaryIndex=ordered.first()
        val secondary=ordered.drop(1)
        val primaryRight=if(secondary.isEmpty())outerRight else 630
        val primaryLeft=if(secondary.isEmpty())120 else outerLeft
        val primaryEnd=if(secondary.isEmpty())WIDTH-120 else primaryRight
        placements+=SheetPanelPlacement(primaryIndex,assignedSlots[primaryIndex],
            SheetBounds(primaryLeft,bodyTop,primaryEnd,bodyBottom),true)

        if(secondary.isNotEmpty()) {
            val rightLeft=primaryRight+gap
            val availableWidth=outerRight-rightLeft
            val columns=if(secondary.size==1)1 else 2
            val rows=(secondary.size+columns-1)/columns
            val availableHeight=bodyBottom-bodyTop-gap*(rows-1)
            secondary.forEachIndexed { index,originalIndex ->
                val row=index/columns
                val column=index%columns
                val cellsOnLastRow=if(row==rows-1)
                    secondary.size-(rows-1)*columns else columns
                // A lone last item is centered instead of stranded on the left.
                val effectiveColumns=if(cellsOnLastRow==1)1 else columns
                val effectiveColumn=if(effectiveColumns==1)0 else column
                val cellW=(availableWidth-gap*(effectiveColumns-1))/effectiveColumns
                val x=rightLeft+effectiveColumn*(cellW+gap)
                val top=bodyTop+(availableHeight*row/rows)+gap*row
                val bottom=bodyTop+(availableHeight*(row+1)/rows)+gap*row
                placements+=SheetPanelPlacement(originalIndex,assignedSlots[originalIndex],
                    SheetBounds(x,top,x+cellW,bottom),false)
            }
        }
        // All panels are isolated and preserve their source-index relationship.
        require(placements.size==assignedSlots.size)
        require(placements.map{it.sourceIndex}.toSet().size==assignedSlots.size)
        require(placements.all {
            it.frame.left>=0 && it.frame.top>=0 &&
                it.frame.right<=WIDTH && it.frame.bottom<=HEIGHT
        })
        require(placements.indices.all{i->
            ((i+1) until placements.size).none{j->
                placements[i].frame.overlaps(placements[j].frame)
            }
        })
        return SheetPresentationPlan(title(role),assignedSlots.size,placements,
            SheetBounds(34,2322,WIDTH-34,2490),WIDTH,HEIGHT)
    }
}
