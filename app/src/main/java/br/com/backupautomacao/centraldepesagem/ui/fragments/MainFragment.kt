package br.com.backupautomacao.centraldepesagem.ui.fragments

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
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import br.com.backupautomacao.centraldepesagem.databinding.FragmentMainBinding
import br.com.backupautomacao.centraldepesagem.repository.ConfigRepository
import br.com.backupautomacao.centraldepesagem.ui.viewmodel.MainFragmentViewModel
import br.com.backupautomacao.centraldepesagem.ui.viewmodel.factory.MainViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import net.gotev.speech.Speech


class MainFragment : Fragment() {

  private var _binding: FragmentMainBinding? = null
  private val binding get() = _binding!!
  private val bluetoothAdapter: BluetoothAdapter by lazy {
    val bluetoothManager = activity?.getSystemService(BluetoothManager::class.java)
    bluetoothManager!!.adapter
  }

  private val mainViewModel: MainFragmentViewModel by activityViewModels {
    val configRepository = ConfigRepository(requireActivity())
    Speech.init(requireActivity())
    MainViewModelFactory(configRepository, bluetoothAdapter, Speech.getInstance())
  }

  override fun onCreateView(
    inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View {

    _binding = FragmentMainBinding.inflate(inflater)
    binding.lifecycleOwner = this
    binding.viewModel = mainViewModel

    return binding.root
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    handleMessages()
    handleButtons()
    configureSpinner()

    mainViewModel.getAvailableDevices().observe(viewLifecycleOwner) { deviceList ->
      mainViewModel.connectToDevices(deviceList)
      val devices = deviceList.mapIndexed { i, _ -> "Balança ${i + 1}" }
      ArrayAdapter(
        requireActivity(),
        android.R.layout.simple_spinner_item,
        devices
      ).also { adapter ->
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerScale.adapter = adapter
      }
    }
  }

  private fun configureSpinner() {
    lifecycleScope.launchWhenStarted {
      mainViewModel.scale.collectLatest { value ->
        Log.i("MainFragment", "configureSpinner: $value")
        value?.let {
          Log.i("MainFragment", "configureSpinner: Update spinner $value")
          binding.spinnerScale.setSelection(value)
        }
      }
    }

    binding.spinnerScale.onItemSelectedListener =
      object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(adapterView: AdapterView<*>?, p1: View?, i: Int, p3: Long) {
          mainViewModel.connectedThreads.value?.let { threadList ->
            if (threadList.size > 0) {
              mainViewModel.setActiveThread(i)
            }
          }
        }

        override fun onNothingSelected(p0: AdapterView<*>?) {

        }
      }
  }

  private fun handleButtons() {
    binding.apply {
      buttonFetchProduct.setOnClickListener {
        val code = inputProductCode.text.toString()
        viewModel!!.fetchItem(code)
      }

      buttonPrint.setOnClickListener {
        if (!checkPermissionToReadAndWriteStorage()) {
          requestStoragePermissionResult.launch(
            arrayOf(
              Manifest.permission.READ_EXTERNAL_STORAGE,
              Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
          )
        }
        viewModel!!.makePrint()
      }

      buttonWeighing.setOnClickListener {
        viewModel!!.makeWeighing()
      }

      buttonRecord.setOnClickListener {
        if (!checkPermissionToRecord()) {
          requestRecordPermission.launch(Manifest.permission.RECORD_AUDIO)
        } else {
//          if (!checkBluetoothPermission()) {
//            requestBluetoothPermissions.launch(
//              arrayOf(
//                Manifest.permission.BLUETOOTH_ADMIN,
//                Manifest.permission.BLUETOOTH_CONNECT,
//                Manifest.permission.BLUETOOTH
//              )
//            )
//          }
          viewModel!!.listening()
        }
      }

      buttonLoadPrice.setOnClickListener {
        viewModel!!.loadPrice(10.00)
      }
    }
  }

  private fun handleMessages() {
    mainViewModel.statusMessage.observe(viewLifecycleOwner) { message ->
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
    return resultRead == PackageManager.PERMISSION_GRANTED &&
        resultWrite == PackageManager.PERMISSION_GRANTED
  }

  private fun checkPermissionToRecord(): Boolean {
    val permission =
      ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.RECORD_AUDIO)
    return permission == PackageManager.PERMISSION_GRANTED
  }

  private fun checkBluetoothPermission(): Boolean {
    val permissionBluetooth =
      ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.BLUETOOTH)
    val permissionBluetoothAdmin =
      ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.BLUETOOTH_ADMIN)
    val permissionBluetoothConnect =
      ContextCompat.checkSelfPermission(requireActivity(), Manifest.permission.BLUETOOTH_CONNECT)
    return permissionBluetooth == PackageManager.PERMISSION_GRANTED &&
        permissionBluetoothAdmin == PackageManager.PERMISSION_GRANTED &&
        permissionBluetoothConnect == PackageManager.PERMISSION_GRANTED
  }

  private val requestBluetoothPermissions =
    registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
      val isGranted = permissions.entries.filter { !it.value }
      if (isGranted.isEmpty()) {
        mainViewModel.listening()
      } else {
        isGranted.forEach {
          Log.i("MainFragment", ": Permissões negada ${it.key}")
        }
      }
    }

  private val requestStoragePermissionResult =
    registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
      val isGranted = permissions.entries.filter { !it.value }
      if (isGranted.isEmpty()) {
        mainViewModel.makePrint()
      } else {
        isGranted.forEach {
          Log.i("MainFragment", "Permissões negadas: ${it.key}")
        }
      }
    }

  private val requestRecordPermission = registerForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    if (isGranted) {
      mainViewModel.listening()
    } else {
      Log.i("MainFragment", "Permissão negada")
    }
  }

  override fun onDestroyView() {
    super.onDestroyView()
    _binding = null
  }
}

