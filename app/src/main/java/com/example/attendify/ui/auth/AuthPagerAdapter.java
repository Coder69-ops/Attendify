package com.example.attendify.ui.auth;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class AuthPagerAdapter extends FragmentStateAdapter {

    private static final int NUM_PAGES = 2;
    private static final int LOGIN_PAGE = 0;
    private static final int REGISTER_PAGE = 1;

    public AuthPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return position == LOGIN_PAGE 
                ? new LoginFragment() 
                : new RegisterFragment();
    }

    @Override
    public int getItemCount() {
        return NUM_PAGES;
    }
} 