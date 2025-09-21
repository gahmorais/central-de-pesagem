package br.com.backupautomacao.pesagem_modular.ui.zpl

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import br.com.backupautomacao.pesagem_modular.databinding.FragmentLabelListBinding

class LabelListFragment : Fragment() {

    private var _binding: FragmentLabelListBinding? = null
    private val binding get() = _binding!!

    private val viewModel: LabelViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLabelListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val context = requireContext()
        val assetManager = context.assets

        val labelFiles = assetManager.list("labels")?.filter { it.endsWith(".zpl") } ?: emptyList()

        val labelsFromAssets = labelFiles.map { fileName ->
            val inputStream = assetManager.open("labels/$fileName")
            val zplCode = inputStream.bufferedReader().use { it.readText() }

            ZplLabel(
                name = fileName.removeSuffix(".zpl").replace("_", " ").replaceFirstChar { it.uppercase() },
                zplCode = zplCode
            )
        }

        viewModel.loadLabelsFromAssets(labelsFromAssets)

        viewModel.labels.observe(viewLifecycleOwner) { labels ->
            binding.recyclerZplLabels.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = ZplLabelAdapter(labels, lifecycleScope)
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
