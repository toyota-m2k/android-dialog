package io.github.toyota32k

import android.content.DialogInterface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import io.github.toyota32k.sample.HogeDialog

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val btn = findViewById<Button>(R.id.message_button)
        btn.setOnClickListener {
            //UtMultiSelectionBox.select(this,"hoge", "タイトル", arrayOf("hoge", "fuga", "piyo"), booleanArrayOf(true,false,true), cancelLabel = getString(R.string.cancel))
            HogeDialog().show(this, "hoge")
        }
    }
}