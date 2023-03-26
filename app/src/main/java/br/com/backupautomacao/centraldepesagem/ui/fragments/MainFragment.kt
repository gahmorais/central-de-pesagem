package br.com.backupautomacao.centraldepesagem.ui.fragments

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
import androidx.fragment.app.viewModels
import androidx.work.WorkInfo
import androidx.work.WorkManager
import br.com.backupautomacao.centraldepesagem.AppApplication
import br.com.backupautomacao.centraldepesagem.PrintWorker
import br.com.backupautomacao.centraldepesagem.R
import br.com.backupautomacao.centraldepesagem.databinding.FragmentMainBinding
import br.com.backupautomacao.centraldepesagem.permission.Permission
import br.com.backupautomacao.centraldepesagem.permission.PermissionManager
import br.com.backupautomacao.centraldepesagem.ui.viewmodel.MainFragmentViewModel
import com.google.android.material.snackbar.Snackbar

class MainFragment : Fragment() {

  private var _binding: FragmentMainBinding? = null
  private val binding get() = _binding!!
  private val mainViewModel: MainFragmentViewModel by viewModels()
  private val permissionManager = PermissionManager.from(this)
  private lateinit var workManager: WorkManager
  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {

    _binding = FragmentMainBinding.inflate(inflater)
    binding.lifecycleOwner = this
    binding.viewModel = mainViewModel
    workManager = (requireActivity().application as AppApplication).workManager

    binding.spinnerLoading.btnCancel.setOnClickListener {
      workManager.cancelUniqueWork(PrintWorker.PRINT_TASK)
      stopLoading()
    }

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
        } else {
          error(getString(R.string.error_to_access_bluetooth))
        }
      }
  }

  private fun configureObservers() {
    mainViewModel.getAvailableDevices().observe(viewLifecycleOwner) { deviceList ->
      mainViewModel.connectToDevices(deviceList)
    }

    mainViewModel.labelsList.observe(viewLifecycleOwner) { labels ->
      val spinnerAdapter =
        ArrayAdapter<String>(requireActivity(), R.layout.custom_item_spinner)
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
    binding.viewModel!!.fetchItem(code)
    binding.inputProductCode.setText("")
  }

  private fun blockProduct() {
    mainViewModel.isFixed.value = mainViewModel.isFixed.value?.not()
    mainViewModel.isFixed.value?.let {fixed ->
      if(fixed){
        mainViewModel.showMessage("Produto fixado")
        binding.buttonFixProduct.text = getString(R.string.release_product)

      }else{
        binding.buttonFixProduct.text = getString(R.string.fix_product)
        mainViewModel.showMessage("Produto liberado")
      }
    }
  }

  private fun print() {
    permissionManager
      .request(Permission.Storage)
      .rationale(getString(R.string.message_to_access_storage))
      .checkPermission { granted ->
        if (granted) {
          binding.viewModel!!.print()
          monitoringPrint()
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
        viewModel!!.weighing()
      }

      buttonFixProduct.setOnClickListener {
        blockProduct()
      }
    }
  }

  private fun handleMessages() {
    mainViewModel.statusMessage.observe(viewLifecycleOwner) { message ->
      Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
    }
  }

  private fun monitoringPrint() {
    workManager
      .getWorkInfosForUniqueWorkLiveData(PrintWorker.PRINT_TASK)
      .observe(viewLifecycleOwner) { workInfos ->
        if (workInfos.isNotEmpty()) {
          when (workInfos[0].state) {
            WorkInfo.State.SUCCEEDED -> {
              stopLoading()
              mainViewModel.showMessage(getString(R.string.printing_done))
            }
            WorkInfo.State.FAILED -> {
              stopLoading()
              mainViewModel.showMessage(getString(R.string.printing_error))
            }
            WorkInfo.State.CANCELLED -> {
              stopLoading()
              mainViewModel.showMessage(getString(R.string.printing_cancelled))
            }
            WorkInfo.State.RUNNING -> {
              Log.i("MainFragment", "monitoringPrint: Rodando tarefa")
            }
            WorkInfo.State.ENQUEUED -> {
              startLoading(getString(R.string.printing))
            }
            else -> stopLoading()
          }
        }
      }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}

