package com.example.youtubescrape;


import android.content.Context;
import android.os.CountDownTimer;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.ViewConfiguration;

import androidx.recyclerview.widget.RecyclerView;


public class GlassGestureDetector {

    /**
     * Currently handled gestures.
     */
    public enum Gesture {
        TAP,
        TAP_AND_HOLD,
        TWO_FINGER_TAP,
        SWIPE_FORWARD,
        TWO_FINGER_SWIPE_FORWARD,
        SWIPE_BACKWARD,
        TWO_FINGER_SWIPE_BACKWARD,
        SWIPE_UP,
        TWO_FINGER_SWIPE_UP,
        SWIPE_DOWN,
        TWO_FINGER_SWIPE_DOWN
    }

    /**
     * Listens for the gestures.
     */
    public interface OnGestureListener {


        boolean onGesture(Gesture gesture);

        default boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return false;
        }


        default void onTouchEnded() {
        }
    }

    private static final int VELOCITY_UNIT = 1000;
    private static final int FIRST_FINGER_POINTER_INDEX = 0;
    private static final int SECOND_FINGER_POINTER_INDEX = 1;
    private static final int TAP_AND_HOLD_THRESHOLD_MS = ViewConfiguration.getLongPressTimeout();
    private static final double TAN_ANGLE_DEGREES = Math.tan(Math.toRadians(60));
    static final int SWIPE_DISTANCE_THRESHOLD_PX = 100;
    static final int SWIPE_VELOCITY_THRESHOLD_PX = 100;

    private final int touchSlopSquare;
    final CountDownTimer tapAndHoldCountDownTimer = new CountDownTimer(TAP_AND_HOLD_THRESHOLD_MS,
            TAP_AND_HOLD_THRESHOLD_MS) {
        @Override
        public void onTick(long millisUntilFinished) {}

        @Override
        public void onFinish() {
            isTapAndHoldPerformed = true;
            onGestureListener.onGesture(Gesture.TAP_AND_HOLD);
        }
    };

    private boolean isInTapRegion;
    private boolean isTwoFingerGesture = false;
    private boolean isActionDownPerformed = false;
    private boolean isTapAndHoldPerformed = false;
    private float firstFingerDownX;
    private float firstFingerDownY;
    private float firstFingerLastFocusX;
    private float firstFingerLastFocusY;
    private float firstFingerVelocityX;
    private float firstFingerVelocityY;
    private float firstFingerDistanceX;
    private float firstFingerDistanceY;
    private float secondFingerDownX;
    private float secondFingerDownY;
    private float secondFingerDistanceX;
    private float secondFingerDistanceY;
    private VelocityTracker velocityTracker;
    private MotionEvent currentDownEvent;
    private OnGestureListener onGestureListener;


    public GlassGestureDetector(Context context, OnGestureListener onGestureListener) {
        final int touchSlop = ViewConfiguration.get(context).getScaledTouchSlop();
        touchSlopSquare = touchSlop * touchSlop;
        this.onGestureListener = onGestureListener;
    }

    public boolean onTouchEvent(MotionEvent motionEvent) {
        if (velocityTracker == null) {
            velocityTracker = VelocityTracker.obtain();
        }
        velocityTracker.addMovement(motionEvent);
        boolean handled = false;

        switch (motionEvent.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                tapAndHoldCountDownTimer.start();
                firstFingerDownX = firstFingerLastFocusX = motionEvent.getX();
                firstFingerDownY = firstFingerLastFocusY = motionEvent.getY();
                isActionDownPerformed = true;
                isInTapRegion = true;
                if (currentDownEvent != null) {
                    currentDownEvent.recycle();
                }
                currentDownEvent = MotionEvent.obtain(motionEvent);
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                tapAndHoldCountDownTimer.cancel();
                isTwoFingerGesture = true;
                secondFingerDownX = motionEvent.getX(motionEvent.getActionIndex());
                secondFingerDownY = motionEvent.getY(motionEvent.getActionIndex());
                break;
            case MotionEvent.ACTION_MOVE:
                final float firstFingerFocusX = motionEvent.getX(FIRST_FINGER_POINTER_INDEX);
                final float firstFingerFocusY = motionEvent.getY(FIRST_FINGER_POINTER_INDEX);
                final float scrollX = firstFingerLastFocusX - firstFingerFocusX;
                final float scrollY = firstFingerLastFocusY - firstFingerFocusY;
                firstFingerDistanceX = firstFingerFocusX - firstFingerDownX;
                firstFingerDistanceY = firstFingerFocusY - firstFingerDownY;
                if (motionEvent.getPointerCount() > 1) {
                    secondFingerDistanceX =
                            motionEvent.getX(SECOND_FINGER_POINTER_INDEX) - secondFingerDownX;
                    secondFingerDistanceY =
                            motionEvent.getY(SECOND_FINGER_POINTER_INDEX) - secondFingerDownY;
                }
                if (isInTapRegion) {
                    final float distance = (firstFingerDistanceX * firstFingerDistanceX)
                            + (firstFingerDistanceY * firstFingerDistanceY);
                    float distanceSecondFinger = 0;
                    if (motionEvent.getPointerCount() > 1) {
                        distanceSecondFinger = (secondFingerDistanceX * secondFingerDistanceX)
                                + (secondFingerDistanceY * secondFingerDistanceY);
                    }
                    if (distance > touchSlopSquare || distanceSecondFinger > touchSlopSquare) {
                        tapAndHoldCountDownTimer.cancel();
                        isInTapRegion = false;
                    }
                }
                if ((Math.abs(scrollX) >= 1) || (Math.abs(scrollY) >= 1)) {
                    handled = onGestureListener
                            .onScroll(currentDownEvent, motionEvent, scrollX, scrollY);
                    firstFingerLastFocusX = firstFingerFocusX;
                    firstFingerLastFocusY = firstFingerFocusY;
                }
                break;
            case MotionEvent.ACTION_UP:
                tapAndHoldCountDownTimer.cancel();
                velocityTracker.computeCurrentVelocity(VELOCITY_UNIT);
                firstFingerVelocityX = velocityTracker
                        .getXVelocity(motionEvent.getPointerId(motionEvent.getActionIndex()));
                firstFingerVelocityY = velocityTracker
                        .getYVelocity(motionEvent.getPointerId(motionEvent.getActionIndex()));
                handled = detectGesture();
                onTouchEnded();
                break;
            case MotionEvent.ACTION_CANCEL:
                tapAndHoldCountDownTimer.cancel();
                velocityTracker.recycle();
                velocityTracker = null;
                isInTapRegion = false;
                isTapAndHoldPerformed = false;
                break;
        }
        return handled;
    }

    private boolean detectGesture() {
        if (!isActionDownPerformed) {
            return false;
        }
        if (isTapAndHoldPerformed) {
            return false;
        }
        final double tan =
                firstFingerDistanceX != 0 ? Math.abs(firstFingerDistanceY / firstFingerDistanceX)
                        : Double.MAX_VALUE;

        if (isTwoFingerGesture) {
            final double tanSecondFinger =
                    secondFingerDistanceX != 0 ? Math.abs(secondFingerDistanceY / secondFingerDistanceX)
                            : Double.MAX_VALUE;
            return detectTwoFingerGesture(tan, tanSecondFinger);
        } else {
            return detectOneFingerGesture(tan);
        }
    }

    private boolean detectOneFingerGesture(double tan) {
        if (tan > TAN_ANGLE_DEGREES) {
            if (Math.abs(firstFingerDistanceY) < SWIPE_DISTANCE_THRESHOLD_PX
                    || Math.abs(firstFingerVelocityY) < SWIPE_VELOCITY_THRESHOLD_PX) {
                if (isInTapRegion) {
                    return onGestureListener.onGesture(Gesture.TAP);
                }
            } else if (firstFingerDistanceY < 0) {
                return onGestureListener.onGesture(Gesture.SWIPE_UP);
            } else if (firstFingerDistanceY > 0) {
                return onGestureListener.onGesture(Gesture.SWIPE_DOWN);
            }
        } else {
            if (Math.abs(firstFingerDistanceX) < SWIPE_DISTANCE_THRESHOLD_PX
                    || Math.abs(firstFingerVelocityX) < SWIPE_VELOCITY_THRESHOLD_PX) {
                if (isInTapRegion) {
                    return onGestureListener.onGesture(Gesture.TAP);
                }
            } else if (firstFingerDistanceX < 0) {
                return onGestureListener.onGesture(Gesture.SWIPE_FORWARD);
            } else if (firstFingerDistanceX > 0) {
                return onGestureListener.onGesture(Gesture.SWIPE_BACKWARD);
            }
        }
        return false;
    }

    private boolean detectTwoFingerGesture(double tan, double tanSecondFinger) {
        if (tan > TAN_ANGLE_DEGREES && tanSecondFinger > TAN_ANGLE_DEGREES) {
            if (Math.abs(firstFingerDistanceY) < SWIPE_DISTANCE_THRESHOLD_PX
                    || Math.abs(firstFingerVelocityY) < SWIPE_VELOCITY_THRESHOLD_PX) {
                if (isInTapRegion) {
                    return onGestureListener.onGesture(Gesture.TWO_FINGER_TAP);
                }
            } else if (firstFingerDistanceY < 0 && secondFingerDistanceY < 0) {
                return onGestureListener.onGesture(Gesture.TWO_FINGER_SWIPE_UP);
            } else if (firstFingerDistanceY > 0 && secondFingerDistanceY > 0) {
                return onGestureListener.onGesture(Gesture.TWO_FINGER_SWIPE_DOWN);
            }
        } else {
            if (Math.abs(firstFingerDistanceX) < SWIPE_DISTANCE_THRESHOLD_PX
                    || Math.abs(firstFingerVelocityX) < SWIPE_VELOCITY_THRESHOLD_PX) {
                if (isInTapRegion) {
                    return onGestureListener.onGesture(Gesture.TWO_FINGER_TAP);
                }
            } else if (firstFingerDistanceX < 0 && secondFingerDistanceX < 0) {
                return onGestureListener.onGesture(Gesture.TWO_FINGER_SWIPE_FORWARD);
            } else if (firstFingerDistanceX > 0 && secondFingerDistanceX > 0) {
                return onGestureListener.onGesture(Gesture.TWO_FINGER_SWIPE_BACKWARD);
            }
        }
        return false;
    }

    private void onTouchEnded() {
        isTwoFingerGesture = false;
        if (velocityTracker != null) {
            velocityTracker.recycle();
            velocityTracker = null;
        }
        isActionDownPerformed = false;
        isTapAndHoldPerformed = false;
        onGestureListener.onTouchEnded();
    }
}