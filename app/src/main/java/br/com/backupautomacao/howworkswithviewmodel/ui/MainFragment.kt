package br.com.backupautomacao.howworkswithviewmodel.ui

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import br.com.backupautomacao.howworkswithviewmodel.databinding.FragmentMainBinding
import br.com.backupautomacao.howworkswithviewmodel.ui.viewmodel.MainFragmentViewModel
import br.com.backupautomacao.howworkswithviewmodel.ui.viewmodel.MainFragmentViewModelFactory
import br.com.backupautomacao.howworkswithviewmodel.utils.Preferences


class MainFragment : Fragment() {
  private val TAG = "MainFragment"
  private lateinit var binding: FragmentMainBinding
  private val bluetoothAdapter: BluetoothAdapter by lazy {
    val bluetoothManager = activity?.getSystemService(BluetoothManager::class.java)
    bluetoothManager!!.adapter
  }
  private val viewModel: MainFragmentViewModel by lazy {
    val preferences = Preferences(requireActivity())
    val factory = MainFragmentViewModelFactory(preferences, bluetoothAdapter)
    ViewModelProvider(this, factory)[MainFragmentViewModel::class.java]
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {

    binding = FragmentMainBinding.inflate(inflater)
    binding.lifecycleOwner = this
    binding.viewModel = viewModel

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val viewModelInstance = binding.viewModel!!
    handleMessages(viewModelInstance)
    handleButtons(viewModelInstance)
    configureSpinner()

    viewModelInstance.getAvailableDevices().observe(viewLifecycleOwner) { deviceList ->
      viewModelInstance.connectToDevices(deviceList)
      val devices = deviceList.mapIndexed { i, _ -> "Balança ${i + 1}" }
      ArrayAdapter(
        requireActivity(),
        android.R.layout.simple_spinner_item,
        devices
      ).also { adapter ->
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_item)
        binding.spinnerScale.adapter = adapter
      }
    }
  }

  private fun configureSpinner() {
    binding.spinnerScale.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
      override fun onItemSelected(adapterView: AdapterView<*>?, p1: View?, index: Int, p3: Long) {
        viewModel.connectedThreads.value?.let { threadList ->
          if (threadList.size > 0) {
            viewModel.setActiveThread(threadList[index])
          }
        }
      }

      override fun onNothingSelected(p0: AdapterView<*>?) {

      }
    }
  }

  private fun handleButtons(viewModel: MainFragmentViewModel) {
    binding.buttonFetchProduct.setOnClickListener {
      val code = binding.inputProductCode.text.toString()
      viewModel.fetchItem(code)
    }

    binding.buttonPrint.setOnClickListener {
      if (!checkPermissionToReadAndWriteStorage()) {
        requestPermissionResult.launch(
          arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
          )
        )
      }
      viewModel.makePrint()
    }

    binding.buttonWeighing.setOnClickListener {
      viewModel.makeWeighing()
    }
  }

  private fun handleMessages(viewModel: MainFragmentViewModel) {
    viewModel.statusMessage.observe(viewLifecycleOwner) { message ->
      Toast.makeText(activity, message, Toast.LENGTH_SHORT).show()
    }
  }

  private fun checkPermissionToReadAndWriteStorage(): Boolean {
    val resultRead = ContextCompat.checkSelfPermission(
      requireActivity(),
      Manifest.permission.READ_EXTERNAL_STORAGE
    )
    val resultWrite = ContextCompat.checkSelfPermission(
      requireActivity(),
      Manifest.permission.WRITE_EXTERNAL_STORAGE
    )
    return resultRead == PackageManager.PERMISSION_GRANTED && resultWrite == PackageManager.PERMISSION_GRANTED
  }

  private val requestPermissionResult =
    registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
      val isGranted = permissions.entries.filter { !it.value }
      if (isGranted.isEmpty()) {
        viewModel.makePrint()
      } else {
        isGranted.forEach {
          Log.i(TAG, "Permissões negadas: ${it.key}")
        }
      }
    }


}