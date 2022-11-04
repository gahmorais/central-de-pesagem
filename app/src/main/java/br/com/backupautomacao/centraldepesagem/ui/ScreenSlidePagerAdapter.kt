package br.com.backupautomacao.centraldepesagem.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import br.com.backupautomacao.centraldepesagem.ui.fragments.MainFragment
import br.com.backupautomacao.centraldepesagem.ui.fragments.ReportFragment
import br.com.backupautomacao.centraldepesagem.ui.fragments.SettingsFragment

private const val NUM_PAGES = 3

class ScreenSlidePagerAdapter(fragmentActivity: FragmentActivity) :
  FragmentStateAdapter(fragmentActivity) {

  override fun getItemCount(): Int = NUM_PAGES

  override fun createFragment(position: Int): Fragment {
    if (position == 0) {
      return MainFragment()
    } else if (position == 1) {
      return SettingsFragment()
    }
    return ReportFragment()
  }
}