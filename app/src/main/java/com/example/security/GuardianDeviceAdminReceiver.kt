package com.example.security

import android.app.admin.DeviceAdminReceiver

/** Receives Android's device-admin lifecycle callbacks. Policy calls are separately gated
 * by DevicePolicyManager.isDeviceOwnerApp(), so enabling legacy admin alone grants no powers. */
class GuardianDeviceAdminReceiver : DeviceAdminReceiver()
