package com.video.downloader.downloadvideo.videodownloader.core.base

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewbinding.ViewBinding
import com.video.downloader.downloadvideo.videodownloader.core.utils.SystemUtil
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

abstract class BaseActivity<VB : ViewBinding> : AppCompatActivity() {

    private var _binding: VB? = null
    protected val binding: VB get() = _binding!!

    abstract fun inflateBinding(inflater: LayoutInflater): VB

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(SystemUtil.wrapContext(newBase))
    }

    override fun applyOverrideConfiguration(overrideConfiguration: android.content.res.Configuration?) {
        if (overrideConfiguration != null) {
            val lang = SystemUtil.getPreLanguage(this)
            val locale = SystemUtil.getLocaleFromCode(lang)
            overrideConfiguration.setLocale(locale)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                overrideConfiguration.setLocales(android.os.LocaleList(locale))
            }
        }
        super.applyOverrideConfiguration(overrideConfiguration)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = inflateBinding(layoutInflater)
        setContentView(binding.root)
        initViews()
        observeData()
    }

    abstract fun initViews()
    open fun observeData() {}

    protected fun <T> collectLifecycleFlow(
        flow: Flow<T>,
        collect: suspend (T) -> Unit
    ) {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                flow.collect(collect)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
