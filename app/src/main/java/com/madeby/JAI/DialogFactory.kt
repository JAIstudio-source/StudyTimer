package com.madeby.JAI

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.InputType
import android.view.Gravity
import android.view.Window
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

object DialogFactory {

    private fun dp(context: Context, value: Int): Int {
        return (value * context.resources.displayMetrics.density).toInt()
    }

    private fun tintedColor(color: Int, alpha: Int): Int {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        return Color.argb(alpha, r, g, b)
    }

    /**
     * Creates a standardized base Dialog styled with the app's ThemeCoordinator.
     */
    fun createBaseDialog(
        context: Context,
        themeCoordinator: ThemeCoordinator
    ): Pair<Dialog, LinearLayout> {
        val dialog = Dialog(context)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            background = themeCoordinator.createDialogBackground(24f)
            setPadding(dp(context, 22), dp(context, 20), dp(context, 22), dp(context, 20))
        }

        dialog.setContentView(root)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return Pair(dialog, root)
    }

    /**
     * Shows a standardized confirmation dialog (e.g. Delete confirm, Reset confirm).
     */
    fun showConfirmationDialog(
        context: Context,
        themeCoordinator: ThemeCoordinator,
        title: String,
        message: String,
        positiveText: String = "Confirm",
        negativeText: String = "Cancel",
        isDestructive: Boolean = false,
        onConfirm: () -> Unit
    ): Dialog {
        val (dialog, root) = createBaseDialog(context, themeCoordinator)

        // Title
        root.addView(TextView(context).apply {
            text = title
            setTextColor(themeCoordinator.textColor)
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
        })

        // Message
        root.addView(TextView(context).apply {
            text = message
            setTextColor(themeCoordinator.textColor)
            alpha = 0.85f
            textSize = 13f
            setPadding(0, dp(context, 10), 0, dp(context, 18))
        })

        // Buttons
        val btnRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }

        // Negative Button
        btnRow.addView(TextView(context).apply {
            text = negativeText
            setTextColor(themeCoordinator.textColor)
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(context, 16), dp(context, 10), dp(context, 16), dp(context, 10))
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 16f)
            setOnClickListener { dialog.dismiss() }
        })

        // Positive Button
        val posBgColor = if (isDestructive) Color.parseColor("#EF4444") else themeCoordinator.primaryColor
        btnRow.addView(TextView(context).apply {
            text = positiveText
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(context, 16), dp(context, 10), dp(context, 16), dp(context, 10))
            background = GradientDrawable().apply {
                cornerRadius = dp(context, 16).toFloat()
                setColor(posBgColor)
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(dp(context, 8), 0, 0, 0)
            }
            setOnClickListener {
                dialog.dismiss()
                onConfirm()
            }
        })

        root.addView(btnRow)
        dialog.show()
        return dialog
    }

    /**
     * Shows a standardized single-input text dialog.
     */
    fun showInputDialog(
        context: Context,
        themeCoordinator: ThemeCoordinator,
        title: String,
        hint: String = "",
        initialValue: String = "",
        inputType: Int = InputType.TYPE_CLASS_TEXT,
        positiveText: String = "Save",
        negativeText: String = "Cancel",
        onConfirm: (String) -> Unit
    ): Dialog {
        val (dialog, root) = createBaseDialog(context, themeCoordinator)

        root.addView(TextView(context).apply {
            text = title
            setTextColor(themeCoordinator.textColor)
            textSize = 18f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(0, 0, 0, dp(context, 14))
        })

        val input = EditText(context).apply {
            setText(initialValue)
            this.hint = hint
            setHintTextColor(tintedColor(themeCoordinator.textColor, 100))
            setTextColor(themeCoordinator.textColor)
            this.inputType = inputType
            textSize = 14f
            setPadding(dp(context, 14), dp(context, 12), dp(context, 14), dp(context, 12))
            background = GradientDrawable().apply {
                cornerRadius = dp(context, 12).toFloat()
                setColor(tintedColor(themeCoordinator.textColor, 20))
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, dp(context, 18))
            }
        }
        root.addView(input)

        val btnRow = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
        }

        btnRow.addView(TextView(context).apply {
            text = negativeText
            setTextColor(themeCoordinator.textColor)
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(context, 16), dp(context, 10), dp(context, 16), dp(context, 10))
            background = themeCoordinator.createGlassChip(tintedColor(themeCoordinator.textColor, 30), 16f)
            setOnClickListener { dialog.dismiss() }
        })

        btnRow.addView(TextView(context).apply {
            text = positiveText
            setTextColor(Color.WHITE)
            textSize = 14f
            typeface = Typeface.create("sans-serif-medium", Typeface.BOLD)
            setPadding(dp(context, 16), dp(context, 10), dp(context, 16), dp(context, 10))
            background = GradientDrawable().apply {
                cornerRadius = dp(context, 16).toFloat()
                setColor(themeCoordinator.primaryColor)
            }
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(dp(context, 8), 0, 0, 0)
            }
            setOnClickListener {
                val value = input.text.toString().trim()
                dialog.dismiss()
                onConfirm(value)
            }
        })

        root.addView(btnRow)
        dialog.show()
        return dialog
    }
}
