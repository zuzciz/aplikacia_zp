package sk.tuke.fina_points_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import com.bumptech.glide.Glide;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;

public abstract class BaseActivity extends AppCompatActivity {

    protected DrawerLayout drawerLayout;
    protected NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_base);

        //DrawerLayout a NavigationView
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.navigation_view);

        //inicializacia Toolbaru
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayUseLogoEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(true);
        }

        //nastavenie hamburger ikony
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        //listener na polozky v menu
        navigationView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_fina_points) {
                startActivity(new Intent(this, MainActivity.class));
                drawerLayout.closeDrawers();
                return true;
            }
            else if (item.getItemId() == R.id.nav_limits) {
                startActivity(new Intent(this, LimitsActivity.class));
                drawerLayout.closeDrawers();
                return true;
            }
            else if (item.getItemId() == R.id.nav_progress) {
                startActivity(new Intent(this, ProgressActivity.class));
                drawerLayout.closeDrawers();
                return true;
            }
            else if (item.getItemId() == R.id.nav_training) {
                startActivity(new Intent(this, TrainingActivity.class));
                drawerLayout.closeDrawers();
                return true;
            }
            else if (item.getItemId() == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                drawerLayout.closeDrawers();
                return true;
            }
            else if (item.getItemId() == R.id.nav_login) {
                startActivity(new Intent(this, LoginActivity.class));
                drawerLayout.closeDrawers();
                return true;
            }
            else if (item.getItemId() == R.id.nav_logout){
                    logout();
                    drawerLayout.closeDrawers();
                    return true;
            }
            return false;
        });

        View headerView = navigationView.getHeaderView(0);
        ImageView profileImageView = headerView.findViewById(R.id.image_profile);
        TextView usernameTextView = headerView.findViewById(R.id.text_username);

        //nacitanie fotky z firebase
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            FirebaseFirestore.getInstance().collection("users")
                    .document(user.getUid())
                    .get()
                    .addOnSuccessListener(documentSnapshot -> {
                        String photoUrl = documentSnapshot.getString("photoUrl");
                        String name = documentSnapshot.getString("name");
                        if (photoUrl != null && !photoUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(photoUrl)
                                    .placeholder(R.drawable.ic_person_placeholder)
                                    .circleCrop()
                                    .into(profileImageView);
                        }if (name != null && !name.isEmpty()) {
                            usernameTextView.setText(name);
                        }
                    });
        } else {
            usernameTextView.setText("Hosť");
        }
    }

    protected void inflateLayout(int layoutResID) {
        FrameLayout frameLayout = findViewById(R.id.content_frame);
        getLayoutInflater().inflate(layoutResID, frameLayout, true);
    }

    private void updateMenuForAuth() {
        boolean loggedIn = FirebaseAuth.getInstance().getCurrentUser() != null;
        Menu menu = navigationView.getMenu();
        menu.findItem(R.id.nav_progress).setVisible(loggedIn);
        menu.findItem(R.id.nav_training).setVisible(loggedIn);
        menu.findItem(R.id.nav_profile).setVisible(loggedIn);
        menu.findItem(R.id.nav_logout).setVisible(loggedIn);
        menu.findItem(R.id.nav_login).setVisible(!loggedIn);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateMenuForAuth();
    }

    protected DocumentReference ownerRef(FirebaseFirestore db, String uid, String swimmerId) {
        DocumentReference userRef = db.collection("users").document(uid);
        return (swimmerId != null && !swimmerId.isEmpty())
                ? userRef.collection("swimmers").document(swimmerId)
                : userRef;
    }
    private void logout() {
        FirebaseAuth.getInstance().signOut();
        Intent intent1 = new Intent(BaseActivity.this, IntroActivity.class);
        startActivity(intent1);
        finish();
    }
}
