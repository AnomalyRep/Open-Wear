package id.openwear.diskhealth.ui.activity

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.progressindicator.LinearProgressIndicator
import id.openwear.diskhealth.R
import id.openwear.diskhealth.data.BackupManager
import id.openwear.diskhealth.data.PartitionItem
import id.openwear.diskhealth.data.Shell
import id.openwear.diskhealth.ui.DialogFactory
import id.openwear.diskhealth.ui.Ui
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Backup / restore screen, ported from v4.0.1 `BackupRestoreActivity`.
 *
 * Backup lists `by-name` partitions (default set first). Restore lists `.img`
 * files from `/storage/emulated/0/Backup` and refuses boot-critical targets.
 */
class BackupRestoreActivity : AppCompatActivity() {

    private lateinit var backupAdapter: PartitionAdapter
    private lateinit var restoreAdapter: PartitionAdapter
    private lateinit var layoutBackup: View
    private lateinit var layoutRestore: View
    private lateinit var toggle: MaterialButtonToggleGroup
    private lateinit var selectAllBackup: MaterialCheckBox
    private lateinit var selectAllRestore: MaterialCheckBox
    private lateinit var advanced: MaterialSwitch
    private lateinit var startBackup: MaterialButton
    private lateinit var startRestore: MaterialButton
    private lateinit var deleteBackups: ImageButton

    private var allPartitions: List<PartitionItem> = emptyList()
    private var restoreItems: List<PartitionItem> = emptyList()
    private var mode = MODE_BACKUP

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Ui.applyTheme(this)
        setContentView(R.layout.activity_backup_restore)
        Ui.edgeToEdge(this)

        val toolbar = findViewById<MaterialToolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        toolbar.setTitle(R.string.backup_restore)
        toolbar.setNavigationIcon(R.drawable.ic_toolbar)
        toolbar.setNavigationOnClickListener { finish() }

        layoutBackup = findViewById(R.id.layout_backup)
        layoutRestore = findViewById(R.id.layout_restore)
        toggle = findViewById(R.id.toggle_group)
        selectAllBackup = findViewById(R.id.cb_select_all_backup)
        selectAllRestore = findViewById(R.id.cb_select_all_restore)
        advanced = findViewById(R.id.switch_advanced_partitions)
        startBackup = findViewById(R.id.btn_start_backup)
        startRestore = findViewById(R.id.btn_start_restore)
        deleteBackups = findViewById(R.id.btn_delete_backup)

        backupAdapter = PartitionAdapter { updateActions() }
        restoreAdapter = PartitionAdapter { updateActions() }
        findViewById<RecyclerView>(R.id.rv_partitions).apply {
            layoutManager = LinearLayoutManager(this@BackupRestoreActivity)
            adapter = backupAdapter
        }
        findViewById<RecyclerView>(R.id.rv_backups).apply {
            layoutManager = LinearLayoutManager(this@BackupRestoreActivity)
            adapter = restoreAdapter
        }

        toggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            mode = if (checkedId == R.id.btn_mode_restore) MODE_RESTORE else MODE_BACKUP
            applyMode()
        }

        advanced.setOnCheckedChangeListener { _, _ -> rebuildBackupList() }
        selectAllBackup.setOnCheckedChangeListener { _, checked ->
            backupAdapter.setAll(checked)
            updateActions()
        }
        selectAllRestore.setOnCheckedChangeListener { _, checked ->
            restoreAdapter.setAll(checked)
            updateActions()
        }

        startBackup.setOnClickListener { confirmBackup() }
        startRestore.setOnClickListener { confirmRestore() }
        deleteBackups.setOnClickListener { confirmDelete() }

        lifecycleScope.launch {
            allPartitions = withContext(Dispatchers.IO) { BackupManager.listPartitions() }
            restoreItems = withContext(Dispatchers.IO) { BackupManager.listBackups() }
            rebuildBackupList()
            restoreAdapter.submit(restoreItems)
            mode = if (intent.getStringExtra("MODE") == "RESTORE") MODE_RESTORE else MODE_BACKUP
            toggle.check(if (mode == MODE_RESTORE) R.id.btn_mode_restore else R.id.btn_mode_backup)
            applyMode()
        }
    }

    private fun rebuildBackupList() {
        val filtered = BackupManager.filterDefaults(allPartitions, advanced.isChecked)
        backupAdapter.submit(filtered)
        selectAllBackup.isChecked = false
        updateActions()
    }

    private fun applyMode() {
        val isBackup = mode == MODE_BACKUP
        layoutBackup.visibility = if (isBackup) View.VISIBLE else View.GONE
        layoutRestore.visibility = if (isBackup) View.GONE else View.VISIBLE
        if (isBackup) {
            selectAllRestore.isChecked = false
            restoreAdapter.clearSelection()
        } else {
            selectAllBackup.isChecked = false
            backupAdapter.clearSelection()
        }
        updateActions()
    }

    private fun updateActions() {
        val backupSelected = backupAdapter.selectedCount()
        val restoreSelected = restoreAdapter.selectedCount()
        startBackup.visibility = if (mode == MODE_BACKUP && backupSelected > 0) View.VISIBLE else View.GONE
        startRestore.visibility = if (mode == MODE_RESTORE && restoreSelected > 0) View.VISIBLE else View.GONE
        deleteBackups.visibility = if (mode == MODE_RESTORE && restoreSelected > 0) View.VISIBLE else View.GONE
    }

    private fun confirmBackup() {
        val selected = backupAdapter.selectedItems()
        if (selected.isEmpty()) {
            Ui.toast(this, getString(R.string.select_at_least_one_partition))
            return
        }
        Ui.dialog(
            this,
            getString(R.string.backup_partition),
            getString(R.string.backup) + " " + selected.size + " " + getString(R.string.partitions_to_backup),
            positive = getString(R.string.yes),
            negative = getString(R.string.close),
            root = findViewById(android.R.id.content),
            onPositive = { performBackup(selected) }
        )
    }

    private fun performBackup(selected: List<PartitionItem>) {
        val dialog = progressDialog()
        lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) {
                BackupManager.backup(this@BackupRestoreActivity, selected) { text ->
                    runOnUiThread {
                        dialog.findViewById<TextView>(R.id.progressText)?.text = text
                    }
                }
            }
            dialog.dismiss()
            if (ok) {
                Ui.toast(this@BackupRestoreActivity, getString(R.string.backup_completed))
                Ui.toast(this@BackupRestoreActivity, getString(R.string.backup_saved))
                restoreItems = withContext(Dispatchers.IO) { BackupManager.listBackups() }
                restoreAdapter.submit(restoreItems)
                selectAllBackup.isChecked = false
                backupAdapter.clearSelection()
                updateActions()
            } else {
                Ui.toast(this@BackupRestoreActivity, "Backup failed. Check root access.")
            }
        }
    }

    private fun confirmRestore() {
        val selected = restoreAdapter.selectedItems()
        if (selected.isEmpty()) {
            Ui.toast(this, getString(R.string.select_at_least_one_file))
            return
        }
        val blocked = selected.any { item ->
            val name = item.name.lowercase()
            BackupManager.restoreBlocked.any { name.startsWith(it) }
        }
        if (blocked) {
            Ui.dialog(
                this,
                getString(R.string.danger),
                getString(R.string.you_are_not_allowed_to_restore),
                positive = getString(R.string.close),
                root = findViewById(android.R.id.content),
                onPositive = { }
            )
            return
        }
        Ui.dialog(
            this,
            getString(R.string.restore_partitions),
            getString(R.string.warning_this_will_overwrite_yo),
            positive = getString(R.string.restore),
            negative = getString(R.string.cancel),
            root = findViewById(android.R.id.content),
            onPositive = { performRestore(selected) }
        )
    }

    private fun performRestore(selected: List<PartitionItem>) {
        val dialog = progressDialog()
        lifecycleScope.launch {
            val ok = withContext(Dispatchers.IO) {
                BackupManager.restore(selected) { text ->
                    runOnUiThread {
                        dialog.findViewById<TextView>(R.id.progressText)?.text = text
                    }
                }
            }
            dialog.dismiss()
            if (ok) {
                Ui.toast(this@BackupRestoreActivity, getString(R.string.restore_completed))
                selectAllRestore.isChecked = false
                restoreAdapter.clearSelection()
                updateActions()
                Ui.dialog(
                    this@BackupRestoreActivity,
                    getString(R.string.reboot),
                    getString(R.string.are_you_sure_you_want_to_reboo),
                    positive = getString(R.string.yes),
                    negative = getString(R.string.no),
                    root = findViewById(android.R.id.content),
                    onPositive = {
                        lifecycleScope.launch(Dispatchers.IO) { Shell.runRoot("reboot", 10) }
                    }
                )
            } else {
                Ui.toast(this@BackupRestoreActivity, "Restore failed. Check root access.")
            }
        }
    }

    private fun confirmDelete() {
        val selected = restoreAdapter.selectedItems()
        if (selected.isEmpty()) return
        Ui.dialog(
            this,
            getString(R.string.delete_backup),
            getString(R.string.are_you_sure_you_want_to_delet) + selected.size +
                getString(R.string.backup_files_this_action_canno),
            positive = getString(R.string.delete),
            negative = getString(R.string.cancel),
            root = findViewById(android.R.id.content),
            onPositive = {
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) { BackupManager.delete(selected) }
                    Ui.toast(this@BackupRestoreActivity, getString(R.string.selected_backups_deleted_succe))
                    restoreItems = withContext(Dispatchers.IO) { BackupManager.listBackups() }
                    restoreAdapter.submit(restoreItems)
                    selectAllRestore.isChecked = false
                    updateActions()
                }
            }
        )
    }

    private fun progressDialog(): android.app.Dialog {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_progress, null)
        view.findViewById<LinearProgressIndicator>(R.id.progressBar).isIndeterminate = true
        view.findViewById<TextView>(R.id.progressText).text = getString(R.string.please_wait)
        val dialog = DialogFactory.create(this, view)
        dialog.setCancelable(false)
        dialog.show()
        return dialog
    }

    private companion object {
        const val MODE_BACKUP = 0
        const val MODE_RESTORE = 1
    }
}

/** RecyclerView adapter with a checkbox per partition / backup file. */
private class PartitionAdapter(
    private val onSelectionChanged: () -> Unit
) : RecyclerView.Adapter<PartitionAdapter.Holder>() {

    private val items = mutableListOf<PartitionItem>()
    private val selected = linkedSetOf<String>()

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val check: android.widget.CheckBox = view.findViewById(R.id.cb_partition)
        val name: TextView = view.findViewById(R.id.tv_partition_name)
    }

    fun submit(list: List<PartitionItem>) {
        items.clear()
        items.addAll(list)
        val names = list.map { it.name }.toSet()
        selected.retainAll(names)
        notifyDataSetChanged()
        onSelectionChanged()
    }

    fun selectedItems(): List<PartitionItem> = items.filter { selected.contains(it.name) }

    fun selectedCount(): Int = selected.size

    fun clearSelection() {
        selected.clear()
        notifyDataSetChanged()
        onSelectionChanged()
    }

    fun setAll(checked: Boolean) {
        selected.clear()
        if (checked) items.forEach { selected.add(it.name) }
        notifyDataSetChanged()
        onSelectionChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_partition, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        holder.name.text = item.name
        holder.check.isChecked = selected.contains(item.name)
        holder.itemView.setOnClickListener {
            if (selected.contains(item.name)) selected.remove(item.name) else selected.add(item.name)
            notifyItemChanged(position)
            onSelectionChanged()
        }
        holder.check.setOnClickListener {
            if (holder.check.isChecked) selected.add(item.name) else selected.remove(item.name)
            onSelectionChanged()
        }
    }

    override fun getItemCount(): Int = items.size
}
