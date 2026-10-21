package id.openwear.diskhealth.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import id.openwear.diskhealth.R
import id.openwear.diskhealth.data.BackupManager
import id.openwear.diskhealth.data.Benchmark
import id.openwear.diskhealth.data.BenchmarkResult
import id.openwear.diskhealth.data.DeviceInfoReader
import id.openwear.diskhealth.data.DiskUsageReader
import id.openwear.diskhealth.data.HistoryPoint
import id.openwear.diskhealth.data.HistoryStore
import id.openwear.diskhealth.data.WearLevelReader
import id.openwear.diskhealth.data.WearNode
import id.openwear.diskhealth.ui.Artwork
import id.openwear.diskhealth.ui.DialogFactory
import id.openwear.diskhealth.ui.Ui
import id.openwear.diskhealth.ui.view.GaugeView
import id.openwear.diskhealth.ui.view.HistoryChartView
import id.openwear.diskhealth.ui.view.PieDiagram
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Dashboard, ported from v4.0.1 `MainActivity` + `p053u1.k/m`.
 *
 * Every resume refreshes the state, matching the original onResume observer.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var gauge: GaugeView
    private lateinit var pie: PieDiagram
    private lateinit var chart: HistoryChartView
    private lateinit var backupList: ListView
    private lateinit var emptyBackup: TextView
    private lateinit var backupSummary: TextView
    private lateinit var runButton: MaterialButton
    private lateinit var writeSpeed: TextView
    private lateinit var readSpeed: TextView
    private lateinit var latency: TextView
    private lateinit var prevWrite: TextView
    private lateinit var prevRead: TextView
    private lateinit var prevLatency: TextView

    private var benchmarkRunning = false
    private var historyA: List<HistoryPoint> = emptyList()
    private var historyB: List<HistoryPoint> = emptyList()

    private fun prefs() =
        getSharedPreferences(packageName + "_preferences", MODE_PRIVATE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Ui.applyTheme(this)
        setContentView(R.layout.activity_main)
        Ui.edgeToEdge(this)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setTitle(R.string.app_name)
        toolbar.setNavigationIcon(R.drawable.ic_toolbar)
        toolbar.setNavigationOnClickListener { }

        runButton = findViewById(R.id.btnRunBenchmark)
        writeSpeed = findViewById(R.id.tvWriteSpeed)
        readSpeed = findViewById(R.id.tvReadSpeed)
        latency = findViewById(R.id.tvLatency)
        prevWrite = findViewById(R.id.tvPrevWriteSpeed)
        prevRead = findViewById(R.id.tvPrevReadSpeed)
        prevLatency = findViewById(R.id.tvPrevLatency)
        gauge = findViewById(R.id.progressBenchmarkGauge)
        pie = findViewById(R.id.pie_chart)
        chart = findViewById(R.id.lineChart)
        backupList = findViewById(R.id.lv_partition_backup)
        emptyBackup = findViewById(R.id.tv_empty_backup)
        backupSummary = findViewById(R.id.tv_backup_summary)

        runButton.setOnClickListener { runBenchmark() }
        findViewById<View>(R.id.btnBackupRestore).setOnClickListener {
            startActivity(Intent(this, BackupRestoreActivity::class.java))
        }
        findViewById<View>(R.id.btn_reports).setOnClickListener { showReports() }

        bindBenchmarkFromPrefs()
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun refresh() {
        lifecycleScope.launch {
            val info = withContext(Dispatchers.IO) { DeviceInfoReader.read(this@MainActivity) }
            withContext(Dispatchers.IO) { WearLevelReader.scan(this@MainActivity) }
            val wear = withContext(Dispatchers.IO) { WearLevelReader.stats(this@MainActivity) }
            val part = withContext(Dispatchers.IO) { DiskUsageReader.read() }
            val a = withContext(Dispatchers.IO) { HistoryStore.load(this@MainActivity, "wear_history_a") }
            val b = withContext(Dispatchers.IO) { HistoryStore.load(this@MainActivity, "wear_history_b") }
            val backups = withContext(Dispatchers.IO) { BackupManager.listBackups().map { it.name } }
            val health = withContext(Dispatchers.IO) { WearLevelReader.healthText(this@MainActivity) }

            historyA = a
            historyB = b

            findViewById<TextView>(R.id.tvDevice).text = info.model.uppercase(Locale.ROOT)
            findViewById<TextView>(R.id.tvCPU).text = getString(R.string.processor) + " : " + info.cpu
            findViewById<TextView>(R.id.tvGPU).text = getString(R.string.graphic) + " : " + info.gpu
            findViewById<TextView>(R.id.tvTotalRam).text = getString(R.string.total_ram) + " : " + info.ram
            findViewById<TextView>(R.id.tvTotalStorage).text = getString(R.string.total_storage) + " : " + info.storage
            findViewById<TextView>(R.id.tvActiveSlot).text = getString(R.string.active_slot) + " : " + info.activeSlot
            findViewById<TextView>(R.id.tvBatteryCapacity).text =
                getString(R.string.battery_capacity) + " : " + info.batteryCapacity + " mAh"
            findViewById<TextView>(R.id.tvKernelPowerProfile).text =
                getString(R.string.power_profile) + " : " + info.powerProfile + " mAh"
            findViewById<TextView>(R.id.tvWearLevel).text = health
            bindArtwork(info.model)

            pie.percent = part.percent.toFloat()
            pie.invalidate()
            findViewById<TextView>(R.id.tv_used_size).text = part.usedText
            findViewById<TextView>(R.id.tv_free_size).text = part.freeText

            findViewById<TextView>(R.id.tv_disk_type).text =
                wear.diskType + " " + getString(R.string.wear_level)
            bindWearGroup(true, wear.groupA.title, wear.groupA.nodePath, wear.groupA.visible, wear.groupA.one, wear.groupA.two)
            bindWearGroup(false, wear.groupB.title, wear.groupB.nodePath, wear.groupB.visible, wear.groupB.one, wear.groupB.two)

            chart.setData(historyA, historyB)
            bindBackups(backups)
            bindBenchmarkFromPrefs()
        }
    }

    private fun bindArtwork(model: String) {
        findViewById<android.widget.ImageView>(R.id.deviceimage)
            .setImageResource(Artwork.forModel(model))
    }

    private fun bindWearGroup(
        isA: Boolean,
        title: String?,
        nodePath: String?,
        visible: Boolean,
        one: WearNode?,
        two: WearNode?
    ) {
        val groupId = if (isA) R.id.wear_a else R.id.wear_b
        val group = findViewById<View>(groupId)
        group.visibility = if (visible) View.VISIBLE else View.GONE
        if (!visible) return

        val titleId = if (isA) R.id.tv_wear_level_title_a else R.id.tv_wear_level_title_b
        val nodeId = if (isA) R.id.tv_node_a else R.id.tv_node_b
        findViewById<TextView>(titleId).text = title.orEmpty()
        findViewById<TextView>(nodeId).text = nodePath.orEmpty()

        if (isA) {
            bindNode(one, R.id.fl_1a, R.id.tv_hex_1_a, R.id.tv_wear_level_percent_1a, R.id.wear_level_progress_1a)
            bindNode(two, R.id.fl_2a, R.id.tv_hex_2_a, R.id.tv_wear_level_percent_2a, R.id.wear_level_progress_2a)
        } else {
            bindNode(one, R.id.fl_1b, R.id.tv_hex_1_b, R.id.tv_wear_level_percent_1b, R.id.wear_level_progress_1b)
            bindNode(two, R.id.fl_2b, R.id.tv_hex_2_b, R.id.tv_wear_level_percent_2b, R.id.wear_level_progress_2b)
        }
    }

    private fun bindNode(node: WearNode?, frameId: Int, hexId: Int, percentId: Int, progressId: Int) {
        val frame = findViewById<View>(frameId)
        if (node == null || !node.visible) {
            frame.visibility = View.GONE
            return
        }
        frame.visibility = View.VISIBLE
        findViewById<TextView>(hexId).text = node.hex
        findViewById<TextView>(percentId).text = node.percentText
        findViewById<com.google.android.material.progressindicator.CircularProgressIndicator>(progressId)
            .setProgressCompat(node.percent, true)
    }

    private fun bindBackups(backups: List<String>) {
        val last = prefs().getLong("last_backup_time", 0L)
        backupSummary.text = if (last > 0) {
            val format = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault())
            format.timeZone = TimeZone.getDefault()
            getString(R.string.last_backup) + " " + format.format(Date(last))
        } else {
            getString(R.string.backup_not_found)
        }

        if (backups.isEmpty()) {
            backupList.visibility = View.GONE
            emptyBackup.visibility = View.VISIBLE
        } else {
            backupList.visibility = View.VISIBLE
            emptyBackup.visibility = View.GONE
            backupList.adapter = ArrayAdapter(this, R.layout.item_list, R.id.tv_file_name, backups)
        }
    }

    private fun bindBenchmarkFromPrefs() {
        val p = prefs()
        val write = p.getString("benchmark_prev_write", "-") ?: "-"
        val read = p.getString("benchmark_prev_read", "-") ?: "-"
        val lat = p.getString("benchmark_prev_latency", "-") ?: "-"

        writeSpeed.text = write
        readSpeed.text = read
        latency.text = lat
        prevWrite.text = getString(R.string.previous_write) + " : " + write
        prevRead.text = getString(R.string.previous_read) + " : " + read
        prevLatency.text = getString(R.string.previous_latency) + " : " + lat
        gauge.animateTo(scoreFromLatency(lat))
        runButton.isEnabled = !benchmarkRunning
        runButton.text = if (benchmarkRunning) "..." else getString(R.string.run_now)
    }

    private fun scoreFromLatency(text: String): Float {
        val digits = text.filter { it.isDigit() }
        if (digits.isEmpty()) return 0f
        val value = digits.toDoubleOrNull() ?: return 0f
        return (((3000.0 - value) / 3000.0) * 100.0).toInt().coerceIn(0, 100).toFloat()
    }

    private fun runBenchmark() {
        if (benchmarkRunning) return
        benchmarkRunning = true
        runButton.isEnabled = false
        runButton.text = "..."
        gauge.animateTo(0f)

        lifecycleScope.launch {
            val result = Benchmark.run(this@MainActivity)
            val write = BenchmarkResult.formatSpeed(result.writeBytesPerSecond)
            val read = BenchmarkResult.formatSpeed(result.readBytesPerSecond)
            val lat = BenchmarkResult.formatLatency(result.latencyMicros)

            prefs().edit()
                .putString("benchmark_prev_write", write)
                .putString("benchmark_prev_read", read)
                .putString("benchmark_prev_latency", lat)
                .apply()

            writeSpeed.text = write
            readSpeed.text = read
            latency.text = lat
            prevWrite.text = getString(R.string.previous_write) + " : " + write
            prevRead.text = getString(R.string.previous_read) + " : " + read
            prevLatency.text = getString(R.string.previous_latency) + " : " + lat
            gauge.animateTo(result.score.toFloat())

            benchmarkRunning = false
            runButton.isEnabled = true
            runButton.setText(R.string.run_now)
        }
    }

    private fun showReports() {
        val root = findViewById<View>(android.R.id.content)
        Ui.blur(root)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_report, null)
        val titleA = view.findViewById<TextView>(R.id.tv_title_a)
        val tableA = view.findViewById<TableLayout>(R.id.table_history_a)
        val divider = view.findViewById<View>(R.id.divider_ab)
        val titleB = view.findViewById<TextView>(R.id.tv_title_b)
        val tableB = view.findViewById<TableLayout>(R.id.table_history_b)

        val aEmpty = historyA.isEmpty()
        val bEmpty = historyB.isEmpty()

        if (aEmpty) {
            titleA.visibility = View.GONE
            tableA.visibility = View.GONE
        } else {
            fillTable(tableA, historyA)
        }
        if (bEmpty) {
            titleB.visibility = View.GONE
            tableB.visibility = View.GONE
        } else {
            fillTable(tableB, historyB)
        }
        divider.visibility = if (aEmpty || bEmpty) View.GONE else View.VISIBLE

        val dialog = DialogFactory.create(this, view)
        view.findViewById<MaterialButton>(R.id.btnClose).setOnClickListener {
            dialog.dismiss()
            Ui.clearBlur(root)
        }
        dialog.setOnDismissListener { Ui.clearBlur(root) }
        dialog.show()
    }

    private fun fillTable(table: TableLayout, points: List<HistoryPoint>) {
        val header = TableRow(this)
        header.addView(cell(getString(R.string.date), bold = true))
        header.addView(cell(getString(R.string.value), bold = true))
        table.addView(header)

        val format = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        for (point in points) {
            val row = TableRow(this)
            row.setPadding(0, 8, 0, 8)
            row.addView(cell(format.format(Date(point.t)), bold = false))
            row.addView(cell(point.v.toString() + "%", bold = false))
            table.addView(row)
        }
    }

    private fun cell(text: String, bold: Boolean): TextView {
        val view = TextView(this)
        view.text = text
        if (bold) {
            view.setTypeface(android.graphics.Typeface.DEFAULT_BOLD)
            view.setPadding(0, 0, 16, 16)
            view.setTextColor(resolveTextColor(android.R.attr.textColorPrimary))
        } else {
            view.setPadding(0, 0, 16, 0)
            view.setTextColor(resolveTextColor(android.R.attr.textColorSecondary))
        }
        return view
    }

    private fun resolveTextColor(attr: Int): Int {
        val value = android.util.TypedValue()
        if (!theme.resolveAttribute(attr, value, true)) return android.graphics.Color.DKGRAY
        return if (value.resourceId != 0) {
            androidx.core.content.ContextCompat.getColor(this, value.resourceId)
        } else {
            value.data
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_toolbar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (item.itemId == R.id.action_settings) {
            startActivity(Intent(this, SettingsActivity::class.java))
            true
        } else {
            super.onOptionsItemSelected(item)
        }
    }
}
