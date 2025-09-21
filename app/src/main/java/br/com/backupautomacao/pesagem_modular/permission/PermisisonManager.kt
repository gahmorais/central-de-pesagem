package br.com.backupautomacao.pesagem_modular.permission

import android.content.pm.PackageManager
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import br.com.backupautomacao.pesagem_modular.R
import java.lang.ref.WeakReference

class PermissionManager private constructor(private val fragment: WeakReference<Fragment>) {
    private val requiredPermissions = mutableListOf<Permission>()
    private var rationale: String? = null
    private var callback: (Boolean) -> Unit = {}
    private var detailedCallback: (Map<Permission, Boolean>) -> Unit = {}

    private val permissionCheck = fragment.get()
        ?.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grantResults ->
            sendResultsAndCleanUp(grantResults)
        }

    companion object {
        fun from(fragment: Fragment) = PermissionManager(WeakReference(fragment))
    }

    fun rationale(description: String): PermissionManager {
        rationale = description
        return this
    }

    fun request(vararg permission: Permission): PermissionManager {
        requiredPermissions.addAll(permission)
        return this
    }

    fun checkPermission(callback: (Boolean) -> Unit) {
        this.callback = callback
        handlePermissionRequest()
    }

    private fun handlePermissionRequest() {
        fragment.get()?.let { fragment ->
            when {
                areAllPermissionGranted(fragment) -> sendPositiveResult()
                shouldShowPermissionRationale(fragment) -> displayRationale(fragment)
                else -> requestPermissions()
            }
        }
    }

    private fun shouldShowPermissionRationale(fragment: Fragment) =
        requiredPermissions.any { it.requiresRationale(fragment) }

    private fun Permission.requiresRationale(fragment: Fragment) =
        permissions.any { fragment.shouldShowRequestPermissionRationale(it) }

    private fun areAllPermissionGranted(fragment: Fragment) =
        requiredPermissions.all { it.isGranted(fragment) }

    private fun Permission.isGranted(fragment: Fragment) =
        permissions.all { hasPermission(fragment, it) }

    private fun hasPermission(fragment: Fragment, permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            fragment.requireContext(),
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun displayRationale(fragment: Fragment) {
        AlertDialog.Builder(fragment.requireContext())
            .setTitle(fragment.getString(R.string.dialog_permission_title))
            .setMessage(rationale ?: fragment.getString(R.string.dialog_permission_default_message))
            .setCancelable(false)
            .setPositiveButton(fragment.getString(R.string.dialog_permission_button_positive)) { _, _ ->
                requestPermissions()
            }.show()
    }

    private fun sendPositiveResult() {
        sendResultsAndCleanUp(getPermissionList().associateWith { true })
    }

    private fun requestPermissions() {
        permissionCheck?.launch(getPermissionList())
    }

    private fun getPermissionList() = requiredPermissions.flatMap {
        it.permissions.toList()
    }.toTypedArray()


    private fun sendResultsAndCleanUp(grantResults: Map<String, Boolean>) {
        callback(grantResults.all { it.value })
        detailedCallback(grantResults.mapKeys { Permission.from(it.key) })
        cleanUp()
    }

    private fun cleanUp() {
        requiredPermissions.clear()
        rationale = null
        callback = {}
        detailedCallback = {}
    }
}