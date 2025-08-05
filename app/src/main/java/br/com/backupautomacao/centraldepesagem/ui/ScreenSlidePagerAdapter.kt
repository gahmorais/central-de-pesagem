package br.com.backupautomacao.centraldepesagem.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import br.com.backupautomacao.centraldepesagem.ui.fragments.MainFragment
import br.com.backupautomacao.centraldepesagem.ui.zpl.ZplLabel
import br.com.backupautomacao.centraldepesagem.ui.fragments.ReportFragment
import br.com.backupautomacao.centraldepesagem.ui.fragments.SettingsFragment
import br.com.backupautomacao.centraldepesagem.ui.zpl.LabelListFragment

private const val NUM_PAGES = 4

class ScreenSlidePagerAdapter(fragmentActivity: FragmentActivity) :
    FragmentStateAdapter(fragmentActivity) {

    override fun getItemCount(): Int = NUM_PAGES

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> MainFragment()
            1 -> LabelListFragment()
            2 -> ReportFragment()
            else -> SettingsFragment()
        }
    }
}