package com.yihengke.robotspeech.view;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.yihengke.robotspeech.R;


/**
 * Created by yu on 2017/3/23.
 */

public class CustomDialog extends Dialog {
    public CustomDialog(Context context, int theme) {
        super(context, theme);

    }

    public CustomDialog(Context context) {
        super(context);
    }

    public CustomDialog(Context context, int theme, int gravity) {
        super(context);
        getWindow().setGravity(gravity);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    public static class Builder {

        private Context context;
        private String title;
        private String message;
        private String positiveButtonText;
        private String negativeButtonText;
        private View contentView;
        private boolean isCancel = true;
        private boolean isCancelOnTouchOutside = true;
        private OnClickListener positiveButtonClickListener,
                negativeButtonClickListener;

        public Builder(Context context) {
            this.context = context;
        }

        /**
         * Set the Dialog message from String
         *
         * @param message
         * @return
         */
        public Builder setMessage(String message) {
            this.message = message;
            return this;
        }

        /**
         * Set the Dialog message from resource
         *
         * @param message
         * @return
         */
        public Builder setMessage(int message) {
            this.message = (String) context.getText(message);
            return this;
        }

        /**
         * Set the Dialog title from resource
         *
         * @param title
         * @return
         */
        public Builder setTitle(int title) {
            this.title = (String) context.getText(title);
            return this;
        }

        /**
         * Set the Dialog title from String
         *
         * @param title
         * @return
         */
        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }

        public Builder setCancelable(boolean cancel) {
            this.isCancel = cancel;
            return this;
        }

        public Builder setCancelOnTouchOutside(boolean isCancelOnTouchOutside) {
            this.isCancelOnTouchOutside = isCancelOnTouchOutside;
            return this;
        }

        /**
         * Set a custom content view for the Dialog. If a message is set, the
         * contentView is not added to the Dialog...
         *
         * @param v
         * @return
         */
        public Builder setContentView(View v) {
            this.contentView = v;
            return this;
        }

        /**
         * Set the positive button resource and it's listener
         *
         * @param positiveButtonText
         * @param listener
         * @return
         */
        public Builder setPositiveButton(int positiveButtonText,
                                         OnClickListener listener) {
            this.positiveButtonText = (String) context
                    .getText(positiveButtonText);
            this.positiveButtonClickListener = listener;
            return this;
        }

        /**
         * Set the positive button text and it's listener
         *
         * @param positiveButtonText
         * @param listener
         * @return
         */
        public Builder setPositiveButton(String positiveButtonText,
                                         OnClickListener listener) {
            this.positiveButtonText = positiveButtonText;
            this.positiveButtonClickListener = listener;
            return this;
        }

        /**
         * Set the negative button resource and it's listener
         *
         * @param negativeButtonText
         * @param listener
         * @return
         */
        public Builder setNegativeButton(int negativeButtonText,
                                         OnClickListener listener) {
            this.negativeButtonText = (String) context
                    .getText(negativeButtonText);
            this.negativeButtonClickListener = listener;
            return this;
        }

        /**
         * Set the negative button text and it's listener
         *
         * @param negativeButtonText
         * @param listener
         * @return
         */
        public Builder setNegativeButton(String negativeButtonText,
                                         OnClickListener listener) {
            this.negativeButtonText = negativeButtonText;
            this.negativeButtonClickListener = listener;
            return this;
        }


        /**
         * Create the custom dialog
         */
        public CustomDialog create() {
            if (context == null)
                return null;
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            // instantiate the dialog with the custom Theme
//            final CustomDialog dialog = new CustomDialog(context,
//                    R.style.customDialog);
            final CustomDialog dialog = new CustomDialog(context,
                    R.style.customDialog);
            View layout = inflater.inflate(R.layout.view_custom_dialog, null);
            dialog.addContentView(layout, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            // set the dialog title
            View topLayout = layout.findViewById(R.id.c_top_line);
            TextView titleTv = ((TextView) layout.findViewById(R.id.c_title3));
            if (title != null) {
                titleTv.setVisibility(View.VISIBLE);
                titleTv.setText(title);
                topLayout.setVisibility(View.VISIBLE);
            } else {
                titleTv.setVisibility(View.GONE);
                topLayout.setVisibility(View.GONE);
            }

            // set the confirm button

            if (!TextUtils.isEmpty(positiveButtonText)) {
                ((TextView) layout.findViewById(R.id.positiveButton))
                        .setText(positiveButtonText);
                if (positiveButtonClickListener != null) {
                    ((TextView) layout.findViewById(R.id.positiveButton))
                            .setOnClickListener(new View.OnClickListener() {
                                public void onClick(View v) {
                                    positiveButtonClickListener.onClick(dialog,
                                            DialogInterface.BUTTON_POSITIVE);
                                }
                            });
                }
            } else {
                // if no confirm button just set the visibility to GONE
                layout.findViewById(R.id.positiveButton).setVisibility(
                        View.GONE);
            }
            // set the cancel button

            if (!TextUtils.isEmpty(negativeButtonText)) {
                ((TextView) layout.findViewById(R.id.negativeButton))
                        .setText(negativeButtonText);
                if (negativeButtonClickListener != null) {
                    ((TextView) layout.findViewById(R.id.negativeButton))
                            .setOnClickListener(new View.OnClickListener() {
                                public void onClick(View v) {
                                    negativeButtonClickListener.onClick(dialog,
                                            DialogInterface.BUTTON_NEGATIVE);
                                }
                            });
                }
            } else {
                // if no confirm button just set the visibility to GONE
                layout.findViewById(R.id.negativeButton).setVisibility(
                        View.GONE);
                layout.findViewById(R.id.middleLine).setVisibility(View.GONE);
                layout.findViewById(R.id.positiveButton).setBackgroundResource(
                        R.drawable.shape_dialog_button);
            }
            if (TextUtils.isEmpty(positiveButtonText)
                    && TextUtils.isEmpty(negativeButtonText)) {
                layout.findViewById(R.id.dialog_line).setVisibility(View.GONE);
            } else {
                layout.findViewById(R.id.dialog_line).setVisibility(
                        View.VISIBLE);
            }
            // set the content message
            if (!TextUtils.isEmpty(message)) {
                ((TextView) layout.findViewById(R.id.message)).setText(message);
            } else if (contentView != null) {
                ((LinearLayout) layout.findViewById(R.id.content))
                        .removeAllViews();
                ((LinearLayout) layout.findViewById(R.id.content))
                        .addView(contentView);
            }
            dialog.setContentView(layout);
            /*DisplayMetrics metrics = new DisplayMetrics();
            ((Activity) context).getWindow().getWindowManager()
                    .getDefaultDisplay().getMetrics(metrics);
            int width = metrics.widthPixels;*/
            dialog.getWindow().setLayout(LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            // dialog.getWindow().setLayout(LayoutParams.MATCH_PARENT,
            // LayoutParams.WRAP_CONTENT);
            // dialog.getWindow().setGravity(Gravity.BOTTOM);
            dialog.setCancelable(isCancel);
            dialog.setCanceledOnTouchOutside(isCancelOnTouchOutside);
            return dialog;
        }

    }

    /**
     * @param c
     * @param title   标题
     * @param msg     中间内容
     * @param ok      确定按钮的文字 为空不显示
     * @param cancel  取消按钮文字 为空不显示
     * @param okl     确定监听
     * @param cancell 取消监听
     * @return
     */
    public static Dialog showDialog(Context c, String title, String msg,
                                    String ok, String cancel, OnClickListener okl,
                                    OnClickListener cancell) {
        Dialog dialog;
        Builder customBuilder = new Builder(c);

        customBuilder.setTitle(title).setMessage(msg)
                .setNegativeButton(cancel, cancell).setPositiveButton(ok, okl);
        dialog = customBuilder.create();
        dialog.show();
        DisplayMetrics metrics = new DisplayMetrics();
        ((Activity) c).getWindow().getWindowManager().getDefaultDisplay()
                .getMetrics(metrics);
        int width = metrics.widthPixels;
        dialog.getWindow().setLayout(4 * width / 5, LinearLayout.LayoutParams.WRAP_CONTENT);
        return dialog;
    }

    /**
     * @param c
     * @param title   标题
     * @param conv    中间内容view
     * @param ok
     * @param cancel
     * @param okl
     * @param cancell
     * @return
     */
    public static Dialog showOtherViewDialog(Context c, String title,
                                             View conv, String ok, String cancel, OnClickListener okl,
                                             OnClickListener cancell) {
        Dialog dialog = null;
        Builder customBuilder = new Builder(c);
        customBuilder.setTitle(title).setContentView(conv)
                .setNegativeButton(cancel, cancell).setPositiveButton(ok, okl);
        dialog = customBuilder.create();
        dialog.show();
        DisplayMetrics metrics = new DisplayMetrics();
        ((Activity) c).getWindow().getWindowManager().getDefaultDisplay()
                .getMetrics(metrics);
        int width = metrics.widthPixels;
        dialog.getWindow().setLayout(4 * width / 5, LinearLayout.LayoutParams.WRAP_CONTENT);
        // dialog.getWindow().setLayout(width, LayoutParams.WRAP_CONTENT);
        return dialog;
    }
}
