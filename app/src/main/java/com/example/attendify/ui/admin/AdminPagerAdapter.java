package com.example.attendify.ui.admin;

import android.util.Log;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class AdminPagerAdapter extends FragmentStateAdapter {
    private static final String TAG = "AdminPagerAdapter";
    private static final int PAGE_COUNT = 4;

    public AdminPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        try {
            Log.d(TAG, "Creating fragment for position: " + position);
            switch (position) {
                case 0:
                    return new LiveAttendanceFragment();
                case 1:
                    return new PendingApprovalsFragment();
                case 2:
                    return new ReportsFragment();
                case 3:
                    return new OfficeManagementFragment();
                default:
                    Log.w(TAG, "Invalid position requested: " + position + ", defaulting to LiveAttendanceFragment");
                    return new LiveAttendanceFragment(); // Default to the first tab as fallback
            }
        } catch (Exception e) {
            Log.e(TAG, "Error creating fragment at position " + position + ": " + e.getMessage(), e);
            // Create an empty fallback fragment to prevent crash
            return new EmptyFragment();
        }
    }

    @Override
    public int getItemCount() {
        return PAGE_COUNT;
    }

    public String getPageTitle(int position) {
        switch (position) {
            case 0:
                return "Live Attendance";
            case 1:
                return "Pending Approvals";
            case 2:
                return "Reports";
            case 3:
                return "Offices";
            default:
                return "Unknown";
        }
    }
}

/**
 * Empty fragment used as a fallback when normal fragment creation fails
 */
class EmptyFragment extends Fragment {
    // Empty implementation will just show a blank screen instead of crashing
}