package net.xpece.android.support.preference;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.Process;
import android.provider.MediaStore;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Created by Eugen on 14.12.2015.
 */
public final class RingtoneManagerCompat extends RingtoneManager {
    private static final String TAG = RingtoneManagerCompat.class.getSimpleName();

    private static final Field FIELD_CURSOR;
    private static final Method METHOD_GET_INTERNAL_RINGTONES;
    private static final Method METHOD_GET_MEDIA_RINGTONES;

    private final RingtoneManagerImpl mImpl;
    private final Context mContext;

    static {
        Field cursor = null;
        try {
            cursor = RingtoneManager.class.getDeclaredField("mCursor");
            cursor.setAccessible(true);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        FIELD_CURSOR = cursor;

        Method getInternalRingtones = null;
        try {
            getInternalRingtones = RingtoneManager.class.getDeclaredMethod("getInternalRingtones");
            getInternalRingtones.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        METHOD_GET_INTERNAL_RINGTONES = getInternalRingtones;

        Method getMediaRingtones = null;
        try {
            getMediaRingtones = RingtoneManager.class.getDeclaredMethod("getMediaRingtones");
            getMediaRingtones.setAccessible(true);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        METHOD_GET_MEDIA_RINGTONES = getMediaRingtones;
    }

    private void setCursorInternal(Cursor cursor) {
        try {
            FIELD_CURSOR.set(this, cursor);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private Cursor getCursorInternal() {
        try {
            return (Cursor) FIELD_CURSOR.get(this);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Cursor getInternalRingtonesInternal() {
        try {
            return (Cursor) METHOD_GET_INTERNAL_RINGTONES.invoke(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private Cursor getMediaRingtonesInternal() {
        try {
            return (Cursor) METHOD_GET_MEDIA_RINGTONES.invoke(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public RingtoneManagerCompat(Activity activity) {
        super(activity);

        mContext = activity;
        if (Build.VERSION.SDK_INT >= 23) {
            mImpl = new RingtoneManagerImplV23();
        } else {
            mImpl = new RingtoneManagerImplBase();
        }
    }

    public RingtoneManagerCompat(Context context) {
        super(context);

        mContext = context;
        if (Build.VERSION.SDK_INT >= 23) {
            mImpl = new RingtoneManagerImplV23();
        } else {
            mImpl = new RingtoneManagerImplBase();
        }
    }

    @Override
    public Cursor getCursor() {
        Cursor mCursor = getCursorInternal();
        if (mCursor != null && mCursor.requery()) {
            return mCursor;
        }

        final Cursor internalCursor = getInternalRingtones();
        final Cursor mediaCursor = getMediaRingtones();

        mCursor = new SortCursor(new Cursor[]{internalCursor, mediaCursor},
            MediaStore.Audio.Media.DEFAULT_SORT_ORDER);
        setCursorInternal(mCursor);
        return mCursor;
    }

    private Cursor getInternalRingtones() {
        return getInternalRingtonesInternal();
    }

    private Cursor getMediaRingtones() {
        return mImpl.getMediaRingtones();
    }

    private interface RingtoneManagerImpl {
        Cursor getMediaRingtones();
    }

    private class RingtoneManagerImplBase implements RingtoneManagerImpl {
        @Override
        public Cursor getMediaRingtones() {
            if (PackageManager.PERMISSION_GRANTED != mContext.checkPermission(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Process.myPid(), Process.myUid())) {
                Log.w(TAG, "No READ_EXTERNAL_STORAGE permission, ignoring ringtones on ext storage");
                return null;
            }
            return getMediaRingtonesInternal();
        }
    }

    private class RingtoneManagerImplV23 implements RingtoneManagerImpl {
        @Override
        public Cursor getMediaRingtones() {
            return getMediaRingtonesInternal();
        }
    }
}
