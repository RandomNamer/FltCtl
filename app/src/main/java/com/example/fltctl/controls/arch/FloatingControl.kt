package com.example.fltctl.controls.arch

import android.content.Context
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry


abstract class FloatingControl: LifecycleOwner, View.OnAttachStateChangeListener {

    override val lifecycle: Lifecycle
        get() =  lifecycleRegistry

    private val lifecycleRegistry = LifecycleRegistry(this)

    protected var rootView: View? = null

    open val tag: String = this::class.simpleName ?: ""

    override fun onViewAttachedToWindow(v: View) {
        dispatchStart()
    }

    override fun onViewDetachedFromWindow(v: View) {
        dispatchStop()
    }

    fun create(context: Context, parent: ViewGroup) {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        onCreate(context)
        rootView?.let { parent.addView(it) }
    }

    private fun dispatchStart() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        onStart()
    }

    private fun dispatchStop() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        onStop()
    }

    fun destroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        rootView?.removeOnAttachStateChangeListener(this)
        onDestroy()
    }

    @CallSuper
    protected open fun onCreate(context: Context) {
        val view = onCreateView(context)
        view.addOnAttachStateChangeListener(this)
        rootView = view
    }

    abstract fun onCreateView(context: Context): View

    @CallSuper
    protected open fun onStart() {

    }

    @CallSuper
    protected open fun onStop() {

    }

    @CallSuper
    protected open fun onDestroy() {

    }

}