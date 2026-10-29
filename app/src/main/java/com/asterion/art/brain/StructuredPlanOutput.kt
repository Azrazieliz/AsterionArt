package com.asterion.art.brain

/**
 * Validates the actual model answer before it is allowed to condition a
 * diffusion model. No raw generated prose is silently promoted to a prompt.
 */
object StructuredPlanOutput {
    data class Result(
        val positive: String,
        val negative: String,
        val sheetPrompts: List<String>,
    )

    private fun section(output:String,tag:String):String {
        val name=Regex.escape(tag)
        return Regex("\\["+name+"\\]([\\s\\S]*?)\\[/"+name+"\\]",
            RegexOption.IGNORE_CASE).find(output)?.groupValues?.get(1)?.trim().orEmpty()
    }

    fun validate(
        output:String,
        baseNegative:String,
        requireThreeCharacterSheets:Boolean=false,
    ):Result {
        require(output.isNotBlank()) { "Reasoning model returned no design" }
        if(requireThreeCharacterSheets) {
            val sheets=(1..3).map { section(output,"SHEET_"+it) }
            require(sheets.all { it.length >= 120 }) {
                "Character design output did not contain three complete frozen sheet descriptions"
            }
            return Result(
                sheets.joinToString("\n\n"),
                section(output,"NEGATIVE").ifBlank { baseNegative },
                sheets,
            )
        }
        val positive=section(output,"POSITIVE")
        require(positive.length >= 80) {
            "Reasoning model did not return a complete [POSITIVE] specification"
        }
        return Result(
            positive,
            section(output,"NEGATIVE").ifBlank { baseNegative },
            emptyList(),
        )
    }
}
