package com.example.punchcardwidget

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/**
 * Um "punch card" individual dentro do widget.
 *
 * @param iconRes nome do drawable a usar quando NÃO marcado (ex: "ic_book_unread")
 * @param iconResChecked nome do drawable a usar quando marcado (ex: "ic_book_read")
 * @param label texto opcional abaixo do ícone. Se null/"" o texto não é exibido.
 * @param checked estado atual (lido / não lido)
 * @param persist se true, o estado é salvo em disco e sobrevive a reinícios do
 *                widget/telefone. Se false, o estado só vive em memória (RAM) e é
 *                perdido quando o processo do widget é encerrado (ex: "Force stop"
 *                ou reinício do aparelho).
 */
data class PunchCardItem(
    val id: Int,
    var iconRes: String,
    var iconResChecked: String,
    var label: String?,
    var checked: Boolean,
    var persist: Boolean
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("iconRes", iconRes)
        put("iconResChecked", iconResChecked)
        put("label", label ?: JSONObject.NULL)
        put("checked", checked)
        put("persist", persist)
    }

    companion object {
        fun fromJson(o: JSONObject): PunchCardItem = PunchCardItem(
            id = o.getInt("id"),
            iconRes = o.getString("iconRes"),
            iconResChecked = o.getString("iconResChecked"),
            label = if (o.isNull("label")) null else o.getString("label"),
            checked = o.getBoolean("checked"),
            persist = o.getBoolean("persist")
        )
    }
}

/**
 * Guarda a lista de cards de CADA instância do widget (appWidgetId), porque o
 * usuário pode colocar mais de um widget na tela, cada um com sua própria
 * configuração.
 *
 * Itens com persist=false são mantidos apenas em um cache em memória
 * (nunca gravados no arquivo de preferências).
 */
object PunchCardStore {

    private const val PREFS_NAME = "punch_card_widget_prefs"
    private fun keyFor(appWidgetId: Int) = "cards_$appWidgetId"

    // cache em memória para os itens não persistentes, por widgetId -> itemId
    private val memoryOnly = mutableMapOf<Int, MutableMap<Int, PunchCardItem>>()

    fun save(context: Context, appWidgetId: Int, items: List<PunchCardItem>) {
        val toDisk = items.filter { it.persist }
        val toMemory = items.filter { !it.persist }

        val mem = memoryOnly.getOrPut(appWidgetId) { mutableMapOf() }
        toMemory.forEach { mem[it.id] = it }

        val array = JSONArray()
        toDisk.forEach { array.put(it.toJson()) }

        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(keyFor(appWidgetId), array.toString())
            .apply()
    }

    fun load(context: Context, appWidgetId: Int): List<PunchCardItem> {
        val diskJson = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(keyFor(appWidgetId), null)

        val diskItems = mutableListOf<PunchCardItem>()
        if (diskJson != null) {
            val array = JSONArray(diskJson)
            for (i in 0 until array.length()) {
                diskItems.add(PunchCardItem.fromJson(array.getJSONObject(i)))
            }
        }

        val memItems = memoryOnly[appWidgetId]?.values.orEmpty()

        return (diskItems + memItems).sortedBy { it.id }
    }

    /** Alterna o estado marcado/não marcado de um card específico. */
    fun toggle(context: Context, appWidgetId: Int, itemId: Int) {
        val items = load(context, appWidgetId).toMutableList()
        val idx = items.indexOfFirst { it.id == itemId }
        if (idx >= 0) {
            items[idx] = items[idx].copy(checked = !items[idx].checked)
            save(context, appWidgetId, items)
        }
    }

    fun clear(context: Context, appWidgetId: Int) {
        memoryOnly.remove(appWidgetId)
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(keyFor(appWidgetId))
            .apply()
    }

    /** Exemplo padrão: 6 livros, sem texto, tudo persistente. */
    fun defaultItems(): List<PunchCardItem> = (1..6).map { i ->
        PunchCardItem(
            id = i,
            iconRes = "ic_book_unread",
            iconResChecked = "ic_book_read",
            label = null,
            checked = false,
            persist = true
        )
    }
}
