package com.dealhunt.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.dealhunt.app.R
import com.dealhunt.app.data.NetworkClient
import com.dealhunt.app.data.TLRate
import com.dealhunt.app.databinding.ActivityDetailBinding
import com.dealhunt.app.model.GameSearchResult
import com.dealhunt.app.util.WishlistManager

class DetailActivity : AppCompatActivity() {
    private lateinit var b: ActivityDetailBinding
    private val vm: DetailViewModel by viewModels()
    private lateinit var adapter: PriceAdapter
    private var gameId = ""
    private var dealId = ""
    private var gameTitle = ""
    private var gameThumb = ""

    override fun onCreate(s: Bundle?) {
        super.onCreate(s)

        // ADIM 1: Binding inflate — burada patlarsa görünür Toast basariz
        try {
            b = ActivityDetailBinding.inflate(layoutInflater)
            setContentView(b.root)
        } catch (e: Throwable) {
            Toast.makeText(this, "Layout hatasi: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        gameId    = intent.getStringExtra("GAME_ID")    ?: ""
        dealId    = intent.getStringExtra("DEAL_ID")    ?: ""
        gameTitle = intent.getStringExtra("GAME_TITLE") ?: "Oyun"
        gameThumb = intent.getStringExtra("GAME_THUMB") ?: ""

        // ADIM 2: Toolbar kurulumu — try-catch ile sarmalandi
        try {
            setSupportActionBar(b.toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = gameTitle
            b.toolbar.setNavigationOnClickListener { finish() }
        } catch (e: Throwable) {
            Toast.makeText(this, "Toolbar hatasi: ${e.message}", Toast.LENGTH_LONG).show()
        }

        // ADIM 3: Glide image - try-catch
        try {
            if (gameThumb.isNotEmpty()) {
                Glide.with(this).load(gameThumb)
                    .placeholder(R.drawable.placeholder_game)
                    .centerCrop().into(b.ivGameHero)
            }
        } catch (e: Throwable) {
            Toast.makeText(this, "Resim hatasi: ${e.message}", Toast.LENGTH_LONG).show()
        }

        // ADIM 4: RecyclerView adapter kurulumu
        try {
            adapter = PriceAdapter { price ->
                try {
                    startActivity(Intent(Intent.ACTION_VIEW,
                        Uri.parse(NetworkClient.dealUrl(price.dealId))))
                } catch (_: Exception) {}
            }
            b.rvPrices.apply {
                layoutManager = LinearLayoutManager(this@DetailActivity)
                adapter = this@DetailActivity.adapter
            }
        } catch (e: Throwable) {
            Toast.makeText(this, "Liste hatasi: ${e.message}", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        b.btnRefresh.setOnClickListener { loadData() }

        try {
            updateFab()
            b.fabWishlist.setOnClickListener {
                val id = gameId.ifBlank { dealId }
                if (id.isBlank()) return@setOnClickListener
                val g = GameSearchResult(gameId = id, title = gameTitle, thumbnail = gameThumb)
                if (WishlistManager.has(this, id)) WishlistManager.remove(this, id)
                else WishlistManager.add(this, g)
                updateFab()
            }
        } catch (e: Throwable) {
            Toast.makeText(this, "Wishlist hatasi: ${e.message}", Toast.LENGTH_LONG).show()
        }

        // ADIM 5: ViewModel observer
        try {
            vm.detail.observe(this) { state ->
                when (state) {
                    is S.Loading -> {
                        b.progressDetail.visibility = View.VISIBLE
                        b.contentLayout.visibility = View.GONE
                        b.tvError.visibility = View.GONE
                    }
                    is S.Ok -> {
                        b.progressDetail.visibility = View.GONE
                        b.contentLayout.visibility = View.VISIBLE
                        b.tvError.visibility = View.GONE
                        try {
                            renderDetail(state.data)
                        } catch (e: Throwable) {
                            Toast.makeText(this, "Render hatasi: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                    is S.Err -> {
                        b.progressDetail.visibility = View.GONE
                        b.contentLayout.visibility = View.GONE
                        b.tvError.visibility = View.VISIBLE
                        b.tvError.text = state.msg
                    }
                    else -> {}
                }
            }
        } catch (e: Throwable) {
            Toast.makeText(this, "Observer hatasi: ${e.message}", Toast.LENGTH_LONG).show()
        }

        loadData()
    }

    private fun renderDetail(d: com.dealhunt.app.model.GameDetailUiState) {
        b.tvGameTitle.text = d.title
        val best = d.platformPrices.firstOrNull()
        if (best != null) {
            b.tvBestPlatform.text = best.storeName
            b.tvBestPrice.text = TLRate.fmt(best.currentPrice)
            if (best.originalPrice > best.currentPrice && best.savingsPercent > 0) {
                b.tvBestDiscount.text = "-%${best.savingsPercent.toInt()}"
                b.tvBestDiscount.visibility = View.VISIBLE
                b.tvBestOriginal.text = TLRate.fmt(best.originalPrice)
                b.tvBestOriginal.visibility = View.VISIBLE
            } else {
                b.tvBestDiscount.visibility = View.GONE
                b.tvBestOriginal.visibility = View.GONE
            }
            b.btnBuyBest.setOnClickListener {
                try {
                    startActivity(Intent(Intent.ACTION_VIEW,
                        Uri.parse(NetworkClient.dealUrl(best.dealId))))
                } catch (_: Exception) {}
            }
        }
        adapter.submitList(d.platformPrices)
        b.tvCheapestEver.text = "${d.cheapestEver} (${d.cheapestEverDate})"
        b.tvPlatformCount.text = "${d.platformPrices.size} platformda mevcut"
    }

    private fun loadData() {
        when {
            gameId.isNotBlank() -> vm.loadByGameId(gameId)
            dealId.isNotBlank() -> vm.loadByDealId(dealId, gameTitle, gameThumb)
            else -> {
                b.progressDetail.visibility = View.GONE
                b.tvError.visibility = View.VISIBLE
                b.tvError.text = "Oyun bilgisi bulunamadi"
            }
        }
    }

    private fun updateFab() {
        val id = gameId.ifBlank { dealId }
        if (id.isBlank()) return
        b.fabWishlist.setImageResource(
            if (WishlistManager.has(this, id)) R.drawable.ic_wishlist_filled
            else R.drawable.ic_wishlist_outline)
    }
}
