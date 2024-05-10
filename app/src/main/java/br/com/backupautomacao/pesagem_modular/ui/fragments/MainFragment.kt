package br.com.backupautomacao.pesagem_modular.ui.fragments

import android.bluetooth.BluetoothManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import br.com.backupautomacao.pesagem_modular.R
import br.com.backupautomacao.pesagem_modular.databinding.FragmentMainBinding
import br.com.backupautomacao.pesagem_modular.permission.Permission
import br.com.backupautomacao.pesagem_modular.permission.PermissionManager
import br.com.backupautomacao.pesagem_modular.ui.viewmodel.MainViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainFragment : Fragment() {

  private val binding: FragmentMainBinding by lazy {
    FragmentMainBinding.inflate(layoutInflater)
  }

  private val mainViewModel: MainViewModel by viewModel()
  private val permissionManager = PermissionManager.from(this)
  private val btAdapter by lazy {
    val btManager = activity?.getSystemService(BluetoothManager::class.java) as BluetoothManager
    btManager.adapter
  }


  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {

    binding.spinnerLoading.btnCancel.setOnClickListener {
      mainViewModel.cancelPrint()
    }

    mainViewModel.monitoringPrintStatus(this)
    mainViewModel.isLoading.onEach { isLoading ->
      if (isLoading) {
        startLoading("Imprimindo")
      } else {
        stopLoading()
      }
    }.launchIn(lifecycleScope)

    mainViewModel.weight.onEach { weight ->

    }.launchIn(lifecycleScope)
    return binding.root
  }

  private fun startLoading(message: String) {
    binding.apply {
      spinnerLoading.tvMessage.text = message
      spinnerLoading.root.visibility = View.VISIBLE
      mainScreen.visibility = View.GONE
    }
  }

  private fun stopLoading() {
    binding.apply {
      spinnerLoading.root.visibility = View.GONE
      mainScreen.visibility = View.VISIBLE
    }
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    bluetoothPermission()
    handleMessages()
    handleButtons()

  }

  private fun bluetoothPermission() {
    val blutoothPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      Permission.BluetoothAndroidS
    } else {
      Permission.Bluetooth
    }
    permissionManager
      .request(blutoothPermission)
      .rationale(getString(R.string.message_to_access_bluetooth))
      .checkPermission { granted ->
        if (granted) {
          configureObservers()
          lifecycleScope.launch { mainViewModel.connectToDevices(btAdapter) }
        } else {
          error(getString(R.string.error_to_access_bluetooth))
        }
      }
  }

  private fun configureObservers() {
    mainViewModel.labelsList.observe(viewLifecycleOwner) { labels ->
      val spinnerAdapter = ArrayAdapter<String>(requireActivity(), R.layout.custom_item_spinner)
      spinnerAdapter.addAll(labels)
      binding.selectLabel.adapter = spinnerAdapter
      binding.selectLabel.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(
          adapterView: AdapterView<*>?,
          view: View?,
          position: Int,
          p3: Long
        ) {
          mainViewModel.updateLabelType(adapterView?.selectedItem.toString())
        }

        override fun onNothingSelected(p0: AdapterView<*>?) {
          TODO("Not yet implemented")
        }
      }
    }
  }

  private fun success(message: String) {
    Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
      .setTextColor(Color.parseColor("#09AF00"))
      .show()
  }

  private fun error(message: String) {
    Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT)
      .setTextColor(Color.parseColor("#B00020"))
      .show()
  }

  private fun getProduct() {
    val code = binding.inputProductCode.text.toString()
    lifecycleScope.launch(Dispatchers.IO) { mainViewModel.fetchItem(code) }
    binding.inputProductCode.setText("")
  }

  private fun blockProduct() {
    mainViewModel.isFixed.value = mainViewModel.isFixed.value?.not()
    mainViewModel.isFixed.value?.let { fixed ->
      if (fixed) {
        mainViewModel.showMessage(getString(R.string.product_blocked))
        binding.buttonFixProduct.text = getString(R.string.release_product)
      } else {
        binding.buttonFixProduct.text = getString(R.string.fix_product)
        mainViewModel.showMessage(getString(R.string.product_unblock))
      }
    }
  }

  private fun print() {
    permissionManager
      .request(Permission.Storage)
      .rationale(getString(R.string.message_to_access_storage))
      .checkPermission { granted ->
        if (granted) {
          lifecycleScope.launch { mainViewModel.print() }
        } else {
          error(getString(R.string.fail_to_access_storage))
        }
      }
  }

  private fun handleButtons() {
    binding.apply {
      buttonPrint.setOnClickListener {
        print()
      }

      inputProductCode.setOnKeyListener { _, keycode, event ->
        if (event.action == KeyEvent.ACTION_DOWN) {
          if (keycode == KeyEvent.KEYCODE_DPAD_CENTER || keycode == KeyEvent.KEYCODE_ENTER) {
            getProduct()
            return@setOnKeyListener true
          }
        }
        false
      }

      buttonWeighing.setOnClickListener {
        val btConnected = mainViewModel.activeDevice.value
        Log.i("MainFragment", "handleButtons: $btConnected")
        btConnected?.let { mainViewModel.weighing(it) }
      }

      buttonFixProduct.setOnClickListener {
        blockProduct()
      }
    }
  }

  private fun handleMessages() {
    mainViewModel.statusMessage.onEach { message ->
      Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
    }.launchIn(lifecycleScope)
  }

}




