package br.com.backupautomacao.centraldepesagem.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import br.com.backupautomacao.centraldepesagem.databinding.FragmentMassProductionBinding

class MassProductionFragment : Fragment() {
  private var _binding: FragmentMassProductionBinding? = null
  private val binding get() = _binding!!


  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return super.onCreateView(inflater, container, savedInstanceState)
  }
  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
  }
}