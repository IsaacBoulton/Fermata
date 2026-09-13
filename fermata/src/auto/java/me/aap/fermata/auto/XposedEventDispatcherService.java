package me.aap.fermata.auto;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.view.MotionEvent;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import me.aap.fermata.FermataApplication;
import me.aap.utils.log.Log;

import java.security.SecureRandom;

public class XposedEventDispatcherService extends Service {
	static final String EXTRA_CALLING_PACKAGE = "callingPackage";
	static final String EXTRA_REGISTRATION_TOKEN = "registrationToken";
	static final int MSG_REGISTER = 0;
	static final int MSG_UNREGISTER = 1;
	static final int MSG_MIRROR_MODE = 2;
	static final int MSG_MOTION_EVENT = 3;
	static final int MSG_BACK_EVENT = 4;
	private static Messenger activityMessenger;
	private static int registrationKey;
	private static int registeredUid = -1;
	private static String expectedPackage;
	private static long expectedToken;
	private final Messenger messenger = new Messenger(new Handler(Looper.getMainLooper()) {
		@Override
		public void handleMessage(@NonNull Message msg) {
			if (msg.what == MSG_REGISTER) {
				if (!isValidRegistration(msg)) {
					Log.e("Rejected event dispatcher registration from uid ", msg.sendingUid);
					return;
				}
				activityMessenger = msg.replyTo;
				registrationKey = msg.arg1;
				registeredUid = msg.sendingUid;
				Log.i("Activity registered for uid ", registeredUid);

				try {
					var mode = FermataApplication.get().getMirroringMode();
					activityMessenger.send(Message.obtain(null, MSG_MIRROR_MODE, mode, 0));
				} catch (RemoteException err) {
					Log.e(err);
				}
			} else if ((msg.what == MSG_UNREGISTER) && (registeredUid == msg.sendingUid) &&
					(registrationKey == msg.arg1)) {
				activityMessenger = null;
				registeredUid = -1;
				Log.i("Activity unregistered");
			}
		}
	});

	private boolean isValidRegistration(Message msg) {
		if ((msg.replyTo == null) || !msg.replyTo.getBinder().isBinderAlive() ||
				!FermataApplication.get().isMirroringMode()) return false;

		String callingPackage = msg.getData().getString(EXTRA_CALLING_PACKAGE);
		if (callingPackage == null) return false;
		String[] packages = getPackageManager().getPackagesForUid(msg.sendingUid);
		if (packages == null) return false;
		boolean ownsPackage = false;
		for (String pkg : packages) {
			if (callingPackage.equals(pkg)) {
				ownsPackage = true;
				break;
			}
		}
		if (!ownsPackage) return false;

		if (!callingPackage.equals(expectedPackage) ||
				(msg.getData().getLong(EXTRA_REGISTRATION_TOKEN) != expectedToken)) return false;

		return (activityMessenger == null) || (registeredUid == msg.sendingUid) ||
				!activityMessenger.getBinder().isBinderAlive();
	}

	static long createRegistrationToken(String packageName) {
		expectedPackage = packageName;
		do {
			expectedToken = new SecureRandom().nextLong();
		} while (expectedToken == 0L);
		return expectedToken;
	}

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		Log.i("Bound: ", intent);
		return messenger.getBinder();
	}

	@Override
	public void onDestroy() {
		activityMessenger = null;
		registeredUid = -1;
		expectedPackage = null;
		expectedToken = 0L;
		super.onDestroy();
	}

	static boolean canDispatchEvent() {
		if ((activityMessenger != null) && activityMessenger.getBinder().isBinderAlive()) return true;
		activityMessenger = null;
		registeredUid = -1;
		return false;
	}

	static boolean dispatchBackEvent() {
		if (!canDispatchEvent()) return false;
		try {
			activityMessenger.send(Message.obtain(null, MSG_BACK_EVENT));
			return true;
		} catch (Exception err) {
			Log.d(err, "Failed to send back event to ", activityMessenger);
			activityMessenger = null;
			registeredUid = -1;
			return false;
		}
	}

	static boolean dispatchEvent(MotionEvent e) {
		if (!canDispatchEvent()) return false;
		try {
			var msg = Message.obtain(null, MSG_MOTION_EVENT);
			var b = new Bundle();
			b.putParcelable("e", e);
			msg.setData(b);
			activityMessenger.send(msg);
			return true;
		} catch (Exception err) {
			Log.d(err, "Failed to send motion event to ", activityMessenger);
			activityMessenger = null;
			registeredUid = -1;
			return false;
		}
	}
}
