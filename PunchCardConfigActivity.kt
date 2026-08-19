package com.example.punchcardwidget

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.NumberPicker
import android.widget.Switch

class PunchCardConfigActivity : Activity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var countPicker: NumberPicker
    private lateinit var cardsContainer: android.widget.LinearLayout

    // guarda referência às views de cada bloco pra ler os valores no "Salvar"
    private data class CardRow(val label: EditText, val showText: Switch, val persist: Switch)
    private val rows = mutableListOf<CardRow>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // se o usuário cancelar, o widget não é adicionado
        setResult(Activity.RESULT_CANCELED)

        setContentView(R.layout.activity_punch_card_config)

        appWidgetId = intent?.extras?.getInt(
            AppWidgetManager.EXTRA_APPWIDGET_ID,
            AppWidgetManager.INVALID_APPWIDGET_ID
        ) ?: AppWidgetManager.INVALID_APPWIDGET_ID

        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        countPicker = findViewById(R.id.count_picker)
        cardsContainer = findViewById(R.id.cards_container)
        countPicker.minValue = 1
        countPicker.maxValue = 20
        countPicker.value = 6
        countPicker.setOnValueChangedListener { _, _, newVal -> renderCardRows(newVal) }

        renderCardRows(countPicker.value)

        findViewById<android.widget.Button>(R.id.save_button).setOnClickListener {
            saveAndFinish()
        }
    }

    private fun renderCardRows(count: Int) {
        cardsContainer.removeAllViews()
        rows.clear()
        val inflater = LayoutInflater.from(this)

        for (i in 1..count) {
            val row = inflater.inflate(R.layout.item_card_config, cardsContainer, false)
            row.findViewById<android.widget.TextView>(R.id.card_title).text = "Cartão $i"
            val label = row.findViewById<EditText>(R.id.label_input)
            val showText = row.findViewById<Switch>(R.id.show_text_switch)
            val persist = row.findViewById<Switch>(R.id.persist_switch)

            cardsContainer.addView(row)
            rows.add(CardRow(label, showText, persist))
        }
    }

    private fun saveAndFinish() {
        val items = rows.mapIndexed { index, row ->
            PunchCardItem(
                id = index + 1,
                iconRes = "ic_book_unread",
                iconResChecked = "ic_book_read",
                label = row.label.text?.toString()?.takeIf { it.isNotBlank() },
                checked = false,
                persist = row.persist.isChecked
            ).let {
                // se "mostrar texto" estiver desligado, ignora o rótulo mesmo que preenchido
                if (!row.showText.isChecked) it.copy(label = null) else it
            }
        }

        PunchCardStore.save(this, appWidgetId, items)

        val appWidgetManager = AppWidgetManager.getInstance(this)
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.punch_card_grid)
        PunchCardWidgetProvider.updateWidget(this, appWidgetManager, appWidgetId)

        val resultValue = Intent().apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        setResult(Activity.RESULT_OK, resultValue)
        finish()
    }
}
