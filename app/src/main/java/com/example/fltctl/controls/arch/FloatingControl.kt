package com.example.fltctl.controls.arch

import android.content.Context
import android.os.Handler
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.example.fltctl.widgets.view.EInkAware


abstract class FloatingControl: LifecycleOwner, View.OnAttachStateChangeListener {

    override val lifecycle: Lifecycle
        get() =  lifecycleRegistry

    private val lifecycleRegistry = LifecycleRegistry(this)

    protected var rootView: View? = null

    open val tag: String = this::class.simpleName ?: ""

    protected var isInEInkMode = false

    val handler: Handler?
        get() = rootView?.handler

    fun setEInkMode(eInk: Boolean) {
        if (isInEInkMode == eInk) return
        isInEInkMode = eInk
        if (rootView != null)  onEInkConfigChanged(eInk)
    }

    override fun onViewAttachedToWindow(v: View) {
        dispatchStart()
    }

    override fun onViewDetachedFromWindow(v: View) {
        dispatchStop()
    }

    fun create(context: Context) {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        onCreate(context)
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
        if (rootView?.isAttachedToWindow == true) {
            (rootView?.parent as? ViewGroup)?.removeView(rootView)
        }
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        rootView?.removeOnAttachStateChangeListener(this)
        onDestroy()
    }

    @CallSuper
    protected open fun onCreate(context: Context) {
        val view = onCreateView(context)
        view.addOnAttachStateChangeListener(this)
        rootView = view
        onViewCreated(view)
    }

    fun getView(): View = rootView ?: throw IllegalStateException("Should be called after onViewCreated")

    abstract fun onCreateView(context: Context): View

    open fun onViewCreated(view: View) {}

    @CallSuper
    protected open fun onStart() {

    }

    @CallSuper
    protected open fun onStop() {

    }

    @CallSuper
    protected open fun onDestroy() {

    }

    @CallSuper
    protected open fun onEInkConfigChanged(isEInk: Boolean) {
        traverseViewTree { v ->
            if (v is EInkAware) {
                v.setEInkState(isEInk)
            }
        }
    }

    final fun traverseViewTree(view: View? = rootView, visitor: (View) -> Unit) {
        if (view != null) {
            visitor(view)
        }
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                traverseViewTree(view.getChildAt(i), visitor)
            }
        }
    }

}