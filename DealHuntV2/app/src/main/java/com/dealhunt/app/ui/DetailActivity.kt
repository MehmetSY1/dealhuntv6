package com.dealhunt.app.ui

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.dealhunt.app.R

class DetailActivity : AppCompatActivity() {

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)

        // En basit haliyle: sadece minimal layout goster, hicbir baska sey yapma
        setContentView(R.layout.activity_detail)

        Toast.makeText(this, "DetailActivity acildi!", Toast.LENGTH_LONG).show()

        try {
            val gameId = intent.getStringExtra("GAME_ID") ?: ""
            val dealId = intent.getStringExtra("DEAL_ID") ?: ""
            val title = intent.getStringExtra("GAME_TITLE") ?: ""

            findViewById<TextView>(R.id.tvGameTitle).text =
                "gameId=$gameId dealId=$dealId title=$title"

        } catch (e: Throwable) {
            Toast.makeText(this, "HATA: ${e.message}", Toast.LENGTH_LONG).show()
            findViewById<TextView>(R.id.tvError)?.apply {
                text = "HATA: ${e}"
                visibility = View.VISIBLE
            }
        }
    }
}
