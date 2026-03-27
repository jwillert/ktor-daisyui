package dev.jwillert.kopetal.components

import kotlinx.html.*

data class TableColumn<T>(
    val header: String,
    val cell: TD.(row: T) -> Unit
)

fun <T> FlowContent.koTable(
    items: List<T>,
    columns: List<TableColumn<T>>,
    zebra: Boolean = true,
    pinRows: Boolean = false,
    hxGet: String? = null,
    hxTarget: String? = null,
    additionalClasses: String = "",
) {
    div {
        classes = setOf("overflow-x-auto")
        table {
            val tableClasses = listOfNotNull(
                "table",
                "table-zebra".takeIf { zebra },
                "table-pin-rows".takeIf { pinRows },
                additionalClasses.takeIf { it.isNotEmpty() }
            ).joinToString(" ")
            this.classes = setOf(tableClasses)

            hxGet?.let { attributes["hx-get"] = it }
            hxTarget?.let { attributes["hx-target"] = it }

            thead {
                tr {
                    columns.forEach { col ->
                        th { +col.header }
                    }
                }
            }
            tbody {
                items.forEach { item ->
                    tr {
                        columns.forEach { col ->
                            td { col.cell(this, item) }
                        }
                    }
                }
            }
        }
    }
}
