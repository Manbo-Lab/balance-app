package com.example.amdbalance

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.InputType
import android.view.View
import android.widget.*
import android.widget.LinearLayout.LayoutParams
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val handler = Handler(Looper.getMainLooper())
    private var autoRefresh: Runnable? = null

    // UI 容器
    private lateinit var balancePage: LinearLayout
    private lateinit var chartPage: LinearLayout
    private lateinit var settingsPage: LinearLayout

    // 余额页控件
    private lateinit var tvRemaining: TextView
    private lateinit var pieChart: PieChartView
    private lateinit var tvUsed: TextView
    private lateinit var tvLimit: TextView
    private lateinit var tvRequests: TextView
    private lateinit var tvReset: TextView
    private lateinit var tvUpdated: TextView
    private lateinit var progressBar: ProgressBar

    // 图表页
    private lateinit var chartContainer: LinearLayout
    private lateinit var tvModelsTitle: TextView

    // 设置页
    private lateinit var editKey: EditText

    private val prefs by lazy { getSharedPreferences("amd_balance", Context.MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bindViews()
        setupTabs()
        setupSettings()
        val key = prefs.getString("api_key", null)
        if (key.isNullOrBlank()) {
            showSettingsHint()
        } else {
            refresh()
            startAutoRefresh()
        }
    }

    private fun bindViews() {
        balancePage = findViewById(R.id.pageBalance)
        chartPage = findViewById(R.id.pageChart)
        settingsPage = findViewById(R.id.pageSettings)

        tvRemaining = findViewById(R.id.tvRemaining)
        pieChart = findViewById(R.id.pieChart)
        tvUsed = findViewById(R.id.tvUsed)
        tvLimit = findViewById(R.id.tvLimit)
        tvRequests = findViewById(R.id.tvRequests)
        tvReset = findViewById(R.id.tvReset)
        tvUpdated = findViewById(R.id.tvUpdated)
        progressBar = findViewById(R.id.progressBar)

        chartContainer = findViewById(R.id.chartContainer)
        tvModelsTitle = findViewById(R.id.tvModelsTitle)

        editKey = findViewById(R.id.editKey)
    }

    private fun setupTabs() {
        val tabs = listOf(
            findViewById<TextView>(R.id.tabBalance),
            findViewById<TextView>(R.id.tabChart),
            findViewById<TextView>(R.id.tabSettings)
        )
        tabs[0].setOnClickListener { showTab(0) }
        tabs[1].setOnClickListener { showTab(1) }
        tabs[2].setOnClickListener { showTab(2) }
        showTab(0)
    }

    private fun showTab(i: Int) {
        balancePage.visibility = if (i == 0) View.VISIBLE else View.GONE
        chartPage.visibility = if (i == 1) View.VISIBLE else View.GONE
        settingsPage.visibility = if (i == 2) View.VISIBLE else View.GONE
    }

    private fun setupSettings() {
        findViewById<Button>(R.id.btnSaveKey).setOnClickListener {
            val key = editKey.text.toString().trim()
            if (key.isEmpty()) {
                toast("请填写 API Key")
                return@setOnClickListener
            }
            prefs.edit().putString("api_key", key).apply()
            editKey.text.clear()
            toast("已保存，正在查询…")
            refresh()
            startAutoRefresh()
            showTab(0)
        }
    }

    private fun startAutoRefresh() {
        autoRefresh?.let { handler.removeCallbacks(it) }
        autoRefresh = object : Runnable {
            override fun run() {
                refresh()
                handler.postDelayed(this, 60_000L)
            }
        }
        handler.postDelayed(autoRefresh!!, 60_000L)
    }

    private fun refresh() {
        val key = prefs.getString("api_key", null)
        if (key.isNullOrBlank()) {
            toast("请先在设置里填写 API Key")
            return
        }
        progressBar.visibility = View.VISIBLE
        scope.launch {
            val result = runCatching { withContext(Dispatchers.IO) { ApiClient.fetchUsage(key) } }
            progressBar.visibility = View.GONE
            result.onSuccess(::render).onFailure {
                toast("查询失败：${it.message}")
                tvUpdated.text = "查询失败"
            }
        }
    }

    private fun render(u: DailyUsage) {
        tvRemaining.text = "$${"%.2f".format(u.remainingUsd)}"
        tvUsed.text = "$${"%.2f".format(u.usedUsd)}"
        tvLimit.text = "$${"%.2f".format(u.limitUsd)}"
        tvRequests.text = u.requests.toString()
        tvReset.text = u.resetZone
        tvUpdated.text = "更新于 ${java.text.SimpleDateFormat("HH:mm:ss").format(java.util.Date())}"

        pieChart.setData(u.usedUsd, u.remainingUsd)

        // 按模型分布条形图
        chartContainer.removeAllViews()
        tvModelsTitle.text = "按模型费用（USD）"
        if (u.models.isEmpty()) {
            val tv = TextView(this).apply { text = "暂无模型数据"; setTextColor(0xFF94A3B8.toInt()) }
            chartContainer.addView(tv)
        } else {
            val max = u.models.maxOf { it.costUsd }.coerceAtLeast(0.001)
            for (m in u.models) {
                chartContainer.addView(modelBar(m.model, m.costUsd, max))
            }
        }
    }

    private fun modelBar(name: String, value: Double, max: Double): View {
        val wrap = LinearLayout(this)
        wrap.orientation = LinearLayout.VERTICAL
        wrap.setPadding(0, dp(6), 0, dp(6))
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = android.view.Gravity.CENTER_VERTICAL
        val label = TextView(this).apply {
            text = "%s  \$%.4f".format(name, value)
            setTextColor(0xFFE2E8F0.toInt())
            textSize = 14f
        }
        row.addView(label, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        wrap.addView(row)
        val bar = View(this)
        bar.setBackgroundColor(0xFF38BDF8.toInt())
        val lp = LayoutParams(LayoutParams.MATCH_PARENT, dp(8))
        lp.topMargin = dp(4)
        wrap.addView(bar, lp)
        bar.post { bar.layoutParams.width = (wrap.width * (value / max)).toInt().coerceAtLeast(4); bar.requestLayout() }
        return wrap
    }

    private fun showSettingsHint() {
        AlertDialog.Builder(this)
            .setTitle("需要 API Key")
            .setMessage("这是 AMD 余额查询工具。首次使用请到「设置」页填写你的 API Key（Bearer Token），它只保存在本机。")
            .setPositiveButton("去填写") { _, _ -> showTab(2); editKey.requestFocus() }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        super.onDestroy()
        autoRefresh?.let { handler.removeCallbacks(it) }
        scope.cancel()
    }
}