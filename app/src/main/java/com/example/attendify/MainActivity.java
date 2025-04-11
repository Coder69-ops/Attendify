package com.example.attendify;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import androidx.core.view.WindowCompat;
import android.view.WindowInsetsController;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.attendify.databinding.ActivityMainBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.transition.platform.MaterialSharedAxis;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        
        // Apply shared element transitions
        getWindow().setEnterTransition(new MaterialSharedAxis(MaterialSharedAxis.Z, true));
        getWindow().setExitTransition(new MaterialSharedAxis(MaterialSharedAxis.Z, true));

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Setup toolbar
        setSupportActionBar(binding.toolbar);

        // Setup bottom navigation
        setupNavigation();
    }

    private void setupNavigation() {
        navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        
        // Define the top-level destinations
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
            R.id.navigation_home,
            R.id.navigation_attendance,
            R.id.navigation_profile,
            R.id.navigation_reports
        ).build();
        
        // Connect toolbar with navigation controller
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        
        // Connect bottom navigation with navigation controller
        NavigationUI.setupWithNavController(binding.bottomNavigation, navController);
        
        // Listen for navigation changes to update UI
        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            // Update UI based on destination if needed
            int destinationId = destination.getId();
            setTitle(destination.getLabel());
        });
        
        // Handle bottom navigation item selection
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_home) {
                navController.navigate(R.id.navigation_home);
                return true;
            } else if (itemId == R.id.navigation_attendance) {
                navController.navigate(R.id.navigation_attendance);
                return true;
            } else if (itemId == R.id.navigation_profile) {
                navController.navigate(R.id.navigation_profile);
                return true;
            } else if (itemId == R.id.navigation_reports) {
                navController.navigate(R.id.navigation_reports);
                return true;
            }
            return false;
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }

    @Override
    public void onBackPressed() {
        // If we're not on the home destination, navigate to home
        if (navController.getCurrentDestination().getId() != R.id.navigation_home) {
            navController.navigate(R.id.navigation_home);
        } else {
            super.onBackPressed();
        }
    }
} 