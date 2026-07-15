package com.bitflaker.lucidsourcekit

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

class ViewPagerAdapter(fragmentManager: FragmentManager, lifecycle: Lifecycle) : FragmentStateAdapter(fragmentManager, lifecycle) {
    private val fragmentArrayList = ArrayList<Fragment>()
    private val fragmentTitle = ArrayList<String>()

    override fun createFragment(position: Int): Fragment = fragmentArrayList[position]
    override fun getItemCount(): Int = fragmentArrayList.size

    fun getFragment(title: String): Fragment = fragmentArrayList[fragmentTitle.indexOf(title)]
    fun getTabIndex(title: String): Int = fragmentTitle.indexOf(title)
    fun isCurrentPage(title: String, position: Int): Boolean = fragmentTitle.size > position && fragmentTitle[position] == title

    fun addFragment(fragment: Fragment, title: String) {
        fragmentArrayList.add(fragment)
        fragmentTitle.add(title)
    }

    fun removeFragment(fragment: Fragment, title: String) {
        fragmentArrayList.remove(fragment)
        fragmentTitle.remove(title)
    }
}