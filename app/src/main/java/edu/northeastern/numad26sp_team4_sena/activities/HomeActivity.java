package edu.northeastern.numad26sp_team4_sena.activities;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.location.CurrentLocationRequest;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.Filter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.northeastern.numad26sp_team4_sena.R;
import edu.northeastern.numad26sp_team4_sena.services.LinkUpListenerService;

public class HomeActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final float DEFAULT_ZOOM = 15f;
    private static final long REQUEST_TIMEOUT_MS = 65_000L;
    private static final double RADIUS_MILES = 10.0;

    private BottomNavigationView bottomNavigation;
    private FrameLayout btnLinkUp;
    private View viewPulseRing, rootView;
    private TextView tvFriendsNearby;
    private ImageButton btnNotifications, btnProfile;
    private FloatingActionButton fabMyLocation;

    private GoogleMap googleMap;
    private FusedLocationProviderClient fusedLocationClient;
    private FirebaseAuth firebaseAuth;
    private FirebaseFirestore firestore;
    private List<Marker> friendMarkers = new ArrayList<>();
    private List<Circle> radiusCircles = new ArrayList<>();
    private View notifBadge;
    private View linkUpOverlay, overlayRing1, overlayRing2;
    private TextView tvOverlaySubtext;
    private ListenerRegistration badgeListener;
    private ListenerRegistration acceptedListener;
    private ListenerRegistration friendMarkersListener;
    private final android.os.Handler timeoutHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable timeoutRunnable;
    private Runnable radiusAnimationRunnable;
    private boolean linkUpInProgress = false;
    private static final long LINKUP_COOLDOWN_MS = 1000;
    private final ActivityResultLauncher<String> locationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    if (googleMap != null) enableMyLocation();
                } else {
                    showSnackbar(getString(R.string.enable_location_prompt));
                }
            });

    private final ActivityResultLauncher<String> notificationPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                requestLocationPermission();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        rootView = findViewById(R.id.main);

        ViewCompat.setOnApplyWindowInsetsListener(rootView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            return insets;
        });

        firebaseAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        initViews();
        setupBottomNavigation();
        setupClickListeners();
        startLinkUpService();
        requestNotificationPermission();
        recoverStaleSessionsAndCleanupRequests();

        SupportMapFragment mapFragment =
                (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void recoverStaleSessionsAndCleanupRequests() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) return;

        final String uid = currentUser.getUid();

        firestore.collection("linkupSessions")
                .where(Filter.or(
                        Filter.equalTo("user1Uid", uid),
                        Filter.equalTo("user2Uid", uid)
                ))
                .whereEqualTo("status", "active")
                .get()
                .addOnSuccessListener(snapshots -> {
                    WriteBatch batch = firestore.batch();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots.getDocuments()) {
                        batch.update(doc.getReference(), "status", "ended");

                        String user1 = doc.getString("user1Uid");
                        String user2 = doc.getString("user2Uid");
                        if (user1 != null) {
                            batch.update(firestore.collection("users").document(user1), "isAvailable", true);
                        }
                        if (user2 != null) {
                            batch.update(firestore.collection("users").document(user2), "isAvailable", true);
                        }
                    }
                    if (!snapshots.isEmpty()) {
                        batch.commit();
                    }
                });

        long twoMinutesAgo = System.currentTimeMillis() - (2 * 60 * 1000);
        firestore.collection("linkupRequests")
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(snapshots -> {
                    WriteBatch batch = firestore.batch();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots.getDocuments()) {
                        com.google.firebase.Timestamp createdAt = doc.getTimestamp("createdAt");
                        if (createdAt != null && createdAt.toDate().getTime() < twoMinutesAgo) {
                            batch.update(doc.getReference(), "status", "expired");
                        }
                    }
                    if (!snapshots.isEmpty()) {
                        batch.commit();
                    }
                });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        googleMap = map;
        googleMap.getUiSettings().setZoomControlsEnabled(false);
        googleMap.getUiSettings().setMyLocationButtonEnabled(false);
        googleMap.getUiSettings().setCompassEnabled(false);
        googleMap.getUiSettings().setMapToolbarEnabled(false);

        if (hasLocationPermission()) {
            enableMyLocation();
        } else {
            LatLng boston = new LatLng(42.3601, -71.0589);
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(boston, DEFAULT_ZOOM));
            loadFriendMarkersOnMap(boston);
        }
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        if (!hasLocationPermission()) {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    @SuppressWarnings("MissingPermission")
    private void enableMyLocation() {
        if (googleMap == null || !hasLocationPermission()) return;

        googleMap.setMyLocationEnabled(true);
        moveCameraToCurrentLocation();
    }

    @SuppressWarnings("MissingPermission")
    private void moveCameraToCurrentLocation() {
        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                onLocationObtained(location);
            } else {
                CurrentLocationRequest request = new CurrentLocationRequest.Builder()
                        .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                        .build();
                fusedLocationClient.getCurrentLocation(request, new CancellationTokenSource().getToken())
                        .addOnSuccessListener(this, fresh -> {
                            if (fresh != null) {
                                onLocationObtained(fresh);
                            } else {
                                LatLng boston = new LatLng(42.3601, -71.0589);
                                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(boston, DEFAULT_ZOOM));
                                loadFriendMarkersOnMap(boston);
                            }
                        });
            }
        });
    }

    private void onLocationObtained(Location location) {
        LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
        googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, DEFAULT_ZOOM));
        saveLocationToFirestore(location.getLatitude(), location.getLongitude());
        drawSearchRadiusCircle(userLatLng);
        loadFriendMarkersOnMap(userLatLng);
    }

    private void saveLocationToFirestore(double latitude, double longitude) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) return;
        Map<String, Object> loc = new HashMap<>();
        loc.put("latitude", latitude);
        loc.put("longitude", longitude);
        firestore.collection("users").document(currentUser.getUid()).update(loc);
    }

    private void loadFriendMarkersOnMap(LatLng myLocation) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null || googleMap == null) return;

        friendMarkersListener = firestore.collection("users").document(currentUser.getUid())
                .addSnapshotListener((userDoc, userError) -> {
                    if (userError != null || userDoc == null || !userDoc.exists()) {
                        updateFriendsNearbyText(0);
                        return;
                    }

                    List<String> friendIds = (List<String>) userDoc.get("friendIds");
                    if (friendIds == null || friendIds.isEmpty()) {
                        for (Marker m : friendMarkers) m.remove();
                        friendMarkers.clear();
                        updateFriendsNearbyText(0);
                        return;
                    }

                    final float nearbyRadiusMeters = (float) (RADIUS_MILES * 1609.34);

                    for (Marker m : friendMarkers) m.remove();
                    friendMarkers.clear();

                    int total = friendIds.size();
                    int[] loaded = {0};
                    int[] nearbyCount = {0};

                    for (String friendId : friendIds) {
                        firestore.collection("users").document(friendId).get()
                                .addOnCompleteListener(task -> {
                                    loaded[0]++;
                                    if (task.isSuccessful() && task.getResult() != null
                                            && task.getResult().exists()) {
                                        Double lat = task.getResult().getDouble("latitude");
                                        Double lng = task.getResult().getDouble("longitude");
                                        if (lat != null && lng != null
                                                && (lat != 0.0 || lng != 0.0)) {
                                            LatLng friendLatLng = new LatLng(lat, lng);
                                            Marker marker = googleMap.addMarker(
                                                    new MarkerOptions()
                                                            .position(friendLatLng)
                                                            .title(getString(R.string.friend_marker_title))
                                                            .icon(BitmapDescriptorFactory.defaultMarker(
                                                                    BitmapDescriptorFactory.HUE_AZURE)));
                                            if (marker != null) friendMarkers.add(marker);

                                            float[] dist = new float[1];
                                            Location.distanceBetween(
                                                    myLocation.latitude, myLocation.longitude,
                                                    lat, lng, dist);
                                            if (dist[0] <= nearbyRadiusMeters) nearbyCount[0]++;
                                        }
                                    }
                                    if (loaded[0] == total) {
                                        updateFriendsNearbyText(nearbyCount[0]);
                                    }
                                });
                    }
                });
    }

    private void drawSearchRadiusCircle(LatLng centerLatLng) {
        if (googleMap == null) return;

        for (Circle circle : radiusCircles) {
            circle.remove();
        }
        radiusCircles.clear();

        if (radiusAnimationRunnable != null) {
            timeoutHandler.removeCallbacks(radiusAnimationRunnable);
        }

        double radiusInMeters = RADIUS_MILES * 1609.34;

        Circle receivingBoundary = googleMap.addCircle(new CircleOptions()
                .center(centerLatLng)
                .radius(radiusInMeters)
                .strokeColor(0xFFFF0000)
                .strokeWidth(4f)
                .fillColor(0x00000000));
        radiusCircles.add(receivingBoundary);

        Circle glowRing1 = googleMap.addCircle(new CircleOptions()
                .center(centerLatLng)
                .radius(radiusInMeters + 50)
                .strokeColor(0x66FF0000)
                .strokeWidth(2f)
                .fillColor(0x00000000));
        radiusCircles.add(glowRing1);


        Circle glowRing2 = googleMap.addCircle(new CircleOptions()
                .center(centerLatLng)
                .radius(radiusInMeters + 100)
                .strokeColor(0x33FF0000)
                .strokeWidth(1f)
                .fillColor(0x00000000));
        radiusCircles.add(glowRing2);

        startRadiusGlowAnimation(centerLatLng, radiusInMeters);
    }

    private void startRadiusGlowAnimation(LatLng centerLatLng, double radiusInMeters) {
        radiusAnimationRunnable = new Runnable() {
            int pulse = 0;

            @Override
            public void run() {
                if (radiusCircles.size() >= 3) {
                    Circle mainCircle = radiusCircles.get(0);
                    Circle glowRing1 = radiusCircles.get(1);
                    Circle glowRing2 = radiusCircles.get(2);

                    pulse = (pulse + 1) % 200;
                    float progress = (float) pulse / 100f;
                    if (progress > 1) progress = 2 - progress;

                    float opacity = 0.8f + (0.2f * progress);
                    mainCircle.setStrokeColor(adjustColorOpacity(0xFFFF0000, opacity));

                    float expandAmount = 20 * progress;
                    glowRing1.setRadius(radiusInMeters + 50 + expandAmount);
                    glowRing2.setRadius(radiusInMeters + 100 + expandAmount);

                    glowRing1.setStrokeColor(adjustColorOpacity(0x66FF0000, 0.5f + (0.5f * progress)));
                    glowRing2.setStrokeColor(adjustColorOpacity(0x33FF0000, 0.3f + (0.3f * progress)));
                }

                timeoutHandler.postDelayed(this, 50);
            }
        };

        timeoutHandler.post(radiusAnimationRunnable);
    }

    private int adjustColorOpacity(int color, float opacity) {
        int alpha = (int) (255 * opacity);
        return (alpha << 24) | (color & 0x00FFFFFF);
    }

    private void initViews() {
        bottomNavigation = findViewById(R.id.bottomNavigation);
        btnLinkUp = findViewById(R.id.btnLinkUp);
        viewPulseRing = findViewById(R.id.viewPulseRing);
        tvFriendsNearby = findViewById(R.id.tvFriendsNearby);
        btnNotifications = findViewById(R.id.btnNotifications);
        btnProfile = findViewById(R.id.btnProfile);
        fabMyLocation = findViewById(R.id.fabMyLocation);
        notifBadge = findViewById(R.id.notifBadge);
        linkUpOverlay = findViewById(R.id.linkUpOverlay);
        overlayRing1 = findViewById(R.id.overlayRing1);
        overlayRing2 = findViewById(R.id.overlayRing2);
        tvOverlaySubtext = findViewById(R.id.tvOverlaySubtext);
        tvFriendsNearby.setText("");

        findViewById(R.id.btnCancelLinkUp).setOnClickListener(v -> cancelLinkUpSearch());
    }

    private void showLinkUpOverlay(int friendCount) {
        tvOverlaySubtext.setText(getString(R.string.request_sent_to_friends, friendCount));

        int[] btnPos = new int[2];
        int[] overlayPos = new int[2];
        btnLinkUp.getLocationOnScreen(btnPos);
        linkUpOverlay.getLocationOnScreen(overlayPos);
        int cx = (btnPos[0] - overlayPos[0]) + btnLinkUp.getWidth() / 2;
        int cy = (btnPos[1] - overlayPos[1]) + btnLinkUp.getHeight() / 2;

        float maxRadius = (float) Math.hypot(linkUpOverlay.getWidth(), linkUpOverlay.getHeight());

        linkUpOverlay.setVisibility(View.VISIBLE);
        Animator reveal = ViewAnimationUtils.createCircularReveal(linkUpOverlay, cx, cy, 0f, maxRadius);
        reveal.setDuration(450);
        reveal.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                Animation ring = AnimationUtils.loadAnimation(HomeActivity.this, R.anim.ripple_ring);
                overlayRing1.startAnimation(ring);
                overlayRing1.postDelayed(() -> {
                    Animation ring2 = AnimationUtils.loadAnimation(HomeActivity.this, R.anim.ripple_ring);
                    overlayRing2.startAnimation(ring2);
                }, 900);
            }
        });
        reveal.start();

        timeoutRunnable = () -> {
            showSnackbar(getString(R.string.no_one_accepted));
            cancelLinkUpSearch();
        };
        timeoutHandler.postDelayed(timeoutRunnable, REQUEST_TIMEOUT_MS);
    }

    private void hideOverlay() {
        if (linkUpOverlay.getVisibility() != View.VISIBLE) return;

        int[] btnPos = new int[2];
        int[] overlayPos = new int[2];
        btnLinkUp.getLocationOnScreen(btnPos);
        linkUpOverlay.getLocationOnScreen(overlayPos);
        int cx = (btnPos[0] - overlayPos[0]) + btnLinkUp.getWidth() / 2;
        int cy = (btnPos[1] - overlayPos[1]) + btnLinkUp.getHeight() / 2;

        float maxRadius = (float) Math.hypot(linkUpOverlay.getWidth(), linkUpOverlay.getHeight());

        if (timeoutRunnable != null) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
            timeoutRunnable = null;
        }

        overlayRing1.clearAnimation();
        overlayRing2.clearAnimation();

        Animator conceal = ViewAnimationUtils.createCircularReveal(linkUpOverlay, cx, cy, maxRadius, 0f);
        conceal.setDuration(350);
        conceal.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                linkUpOverlay.setVisibility(View.GONE);
            }
        });
        conceal.start();
    }

    private void cancelLinkUpSearch() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            hideOverlay();
            return;
        }

        String myUid = currentUser.getUid();
        firestore.collection("linkupRequests")
                .whereEqualTo("fromUid", myUid)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (!snapshots.isEmpty()) {
                        WriteBatch batch = firestore.batch();
                        for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots.getDocuments()) {
                            batch.update(doc.getReference(), "status", "expired");
                        }
                        batch.commit();
                    }
                    setAvailability(true);
                    hideOverlay();
                    linkUpInProgress = false;
                });
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_home);

        bottomNavigation.setOnItemSelectedListener(item -> {
            if (linkUpInProgress) {
                showSnackbar(getString(R.string.cant_navigate_during_linkup));
                return false;
            }

            int id = item.getItemId();
            if (id == R.id.nav_home) {
                return true;
            } else if (id == R.id.nav_search) {
                Intent searchIntent = new Intent(this, SearchActivity.class);
                if (googleMap != null) {
                    com.google.android.gms.maps.model.LatLngBounds bounds = googleMap.getProjection().getVisibleRegion().latLngBounds;
                    if (bounds != null) {
                        searchIntent.putExtra("mapNorthEast", bounds.northeast);
                        searchIntent.putExtra("mapSouthWest", bounds.southwest);
                    }
                }
                startActivity(searchIntent);
                overridePendingTransition(R.anim.fade_in, android.R.anim.fade_out);
                return true;
            } else if (id == R.id.nav_friends) {
                startActivity(new Intent(this, FriendsActivity.class));
                overridePendingTransition(R.anim.fade_in, android.R.anim.fade_out);
                return true;
            }
            return false;
        });
    }

    private void setupClickListeners() {
        btnLinkUp.setOnClickListener(v -> {
            if (linkUpInProgress) {
                showSnackbar(getString(R.string.please_wait));
                return;
            }
            linkUpInProgress = true;
            v.animate().scaleX(0.9f).scaleY(0.9f).setDuration(100)
                    .withEndAction(() -> v.animate().scaleX(1f).scaleY(1f).setDuration(100).start())
                    .start();
            handleLinkUpTap();
            timeoutHandler.postDelayed(() -> linkUpInProgress = false, LINKUP_COOLDOWN_MS);
        });

        btnProfile.setOnClickListener(v -> {
            startActivity(new Intent(this, EditProfileActivity.class));
            overridePendingTransition(R.anim.fade_in, android.R.anim.fade_out);
        });

        btnNotifications.setOnClickListener(v -> {
            startActivity(new Intent(this, NotificationsActivity.class));
            overridePendingTransition(R.anim.fade_in, android.R.anim.fade_out);
        });

        ImageButton btnHistory = findViewById(R.id.btnHistory);
        btnHistory.setOnClickListener(v -> {
            startActivity(new Intent(this, HistoryActivity.class));
            overridePendingTransition(R.anim.fade_in, android.R.anim.fade_out);
        });

        fabMyLocation.setOnClickListener(v -> {
            if (googleMap != null && hasLocationPermission()) {
                moveCameraToCurrentLocation();
            } else if (!hasLocationPermission()) {
                requestLocationPermission();
            }
        });
    }

    private void updateFriendsNearbyText(int count) {
        if (count == 0) {
            tvFriendsNearby.setText(getString(R.string.no_friends_nearby));
        } else {
            tvFriendsNearby.setText(getString(R.string.friends_nearby, count));
        }
    }

    private void startLinkUpService() {
        startForegroundService(new Intent(this, LinkUpListenerService.class));
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS);
        } else {
            requestLocationPermission();
        }
    }

    @SuppressWarnings("MissingPermission")
    private void handleLinkUpTap() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) return;

        if (!hasLocationPermission()) {
            showSnackbar(getString(R.string.location_required));
            return;
        }

        showSnackbar(getString(R.string.looking_for_friend));

        fusedLocationClient.getLastLocation().addOnSuccessListener(this, location -> {
            if (location != null) {
                doLinkUpWithLocation(currentUser.getUid(), location);
            } else {
                CurrentLocationRequest request = new CurrentLocationRequest.Builder()
                        .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                        .build();
                fusedLocationClient.getCurrentLocation(request, new CancellationTokenSource().getToken())
                        .addOnSuccessListener(this, fresh -> {
                            if (fresh != null) {
                                doLinkUpWithLocation(currentUser.getUid(), fresh);
                            } else {
                                showSnackbar(getString(R.string.location_unavailable));
                            }
                        });
            }
        });
    }

    private void doLinkUpWithLocation(String myUid, Location location) {
        double myLat = location.getLatitude();
        double myLng = location.getLongitude();

        firestore.collection("linkupSessions")
                .where(Filter.or(
                        Filter.equalTo("user1Uid", myUid),
                        Filter.equalTo("user2Uid", myUid)
                ))
                .whereEqualTo("status", "active")
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (!snapshots.isEmpty()) {
                        showSnackbar(getString(R.string.error_already_linked_up));
                        return;
                    }

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("latitude", myLat);
                    updates.put("longitude", myLng);
                    updates.put("isAvailable", true);

                    firestore.collection("users").document(myUid).update(updates)
                            .addOnSuccessListener(v -> findNearbyAvailableFriends(myUid, myLat, myLng));
                });
    }

    @SuppressWarnings("unchecked")
    private void findNearbyAvailableFriends(String myUid, double myLat, double myLng) {
        firestore.collection("users").document(myUid).get()
                .addOnSuccessListener(myDoc -> {
                    List<String> friendIds = (List<String>) myDoc.get("friendIds");
                    final double myRadiusMeters = RADIUS_MILES * 1609.34;

                    if (friendIds == null || friendIds.isEmpty()) {
                        showSnackbar(getString(R.string.no_friends_yet_add));
                        setAvailability(true);
                        return;
                    }

                    int total = friendIds.size();
                    int[] loaded = {0};
                    List<String> nearbyUids = new ArrayList<>();

                    for (String fid : friendIds) {
                        firestore.collection("users").document(fid).get()
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful() && task.getResult() != null
                                            && task.getResult().exists()) {
                                        Boolean available = task.getResult().getBoolean("isAvailable");
                                        Double lat = task.getResult().getDouble("latitude");
                                        Double lng = task.getResult().getDouble("longitude");
                                        if (Boolean.TRUE.equals(available)
                                                && lat != null && lng != null
                                                && (lat != 0.0 || lng != 0.0)) {
                                            float[] dist = new float[1];
                                            Location.distanceBetween(myLat, myLng, lat, lng, dist);

                                            if (dist[0] <= myRadiusMeters) {
                                                nearbyUids.add(task.getResult().getId());
                                            }
                                        }
                                    }
                                    loaded[0]++;
                                    if (loaded[0] == total) {
                                        if (nearbyUids.isEmpty()) {
                                            showSnackbar(getString(R.string.no_friends_available));
                                            setAvailability(true);
                                        } else {
                                            createLinkUpRequests(myUid, nearbyUids);
                                        }
                                    }
                                });
                    }
                });
    }

    private void createLinkUpRequests(String myUid, List<String> friendUids) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) return;

        WriteBatch batch = firestore.batch();

        currentUser.reload().addOnSuccessListener(v -> {
            firestore.collection("users").document(myUid).get()
                    .addOnSuccessListener(doc -> {
                        String senderName = doc.getString("fullName");
                        if (senderName == null) senderName = "Someone";

                        for (String friendUid : friendUids) {
                            Map<String, Object> req = new HashMap<>();
                            req.put("fromUid", myUid);
                            req.put("toUid", friendUid);
                            req.put("status", "pending");
                            req.put("senderName", senderName);
                            req.put("createdAt", FieldValue.serverTimestamp());
                            batch.set(firestore.collection("linkupRequests").document(), req);
                        }
                        int count = friendUids.size();
                        batch.commit()
                                .addOnSuccessListener(v2 -> showLinkUpOverlay(count))
                                .addOnFailureListener(e -> {
                                    showSnackbar(getString(R.string.failed_to_send_requests));
                                    setAvailability(true);
                                });
                    });
        });
    }

    private void showSnackbar(String message) {
        Snackbar snackbar = Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT);
        snackbar.setBackgroundTint(getColor(R.color.snackbar_background));
        snackbar.setTextColor(getColor(R.color.white));
        snackbar.setActionTextColor(getColor(R.color.snackbar_action));
        snackbar.setAnchorView(bottomNavigation);
        snackbar.show();
    }

    @Override
    protected void onStart() {
        super.onStart();
        listenForBadge();
        listenForAcceptedRequests();
    }

    @Override
    protected void onResume() {
        super.onResume();
        bottomNavigation.setSelectedItemId(R.id.nav_home);
        ensureAvailableIfNoActiveSession();

        if (googleMap != null) {
            FirebaseUser currentUser = firebaseAuth.getCurrentUser();
            if (currentUser != null && !radiusCircles.isEmpty()) {
                LatLng cameraCenter = googleMap.getCameraPosition().target;
                drawSearchRadiusCircle(cameraCenter);
            }
        }
    }

    private void ensureAvailableIfNoActiveSession() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) return;
        String uid = currentUser.getUid();
        firestore.collection("linkupSessions")
                .where(Filter.or(
                        Filter.equalTo("user1Uid", uid),
                        Filter.equalTo("user2Uid", uid)
                ))
                .whereEqualTo("status", "active")
                .limit(1)
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots.isEmpty()) {
                        setAvailability(true);
                    }
                });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (badgeListener != null) badgeListener.remove();
        if (acceptedListener != null) acceptedListener.remove();
        if (friendMarkersListener != null) friendMarkersListener.remove();
    }

    private void listenForBadge() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) return;
        badgeListener = firestore.collection("linkupRequests")
                .whereEqualTo("toUid", currentUser.getUid())
                .whereEqualTo("status", "pending")
                .addSnapshotListener((snapshots, e) -> {
                    boolean hasPending = snapshots != null && !snapshots.isEmpty();
                    notifBadge.setVisibility(hasPending ? View.VISIBLE : View.GONE);
                });
    }

    private void listenForAcceptedRequests() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) return;
        acceptedListener = firestore.collection("linkupRequests")
                .whereEqualTo("fromUid", currentUser.getUid())
                .addSnapshotListener((snapshots, e) -> {
                    if (snapshots == null || snapshots.isEmpty()) return;

                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == DocumentChange.Type.MODIFIED) {
                            String status = dc.getDocument().getString("status");
                            String sessionId = dc.getDocument().getString("sessionId");
                            if ("accepted".equals(status) && sessionId != null) {
                                hideOverlay();
                                cancelOtherPendingRequests(currentUser.getUid(), dc.getDocument().getId());
                                Intent intent = new Intent(this, ChatActivity.class);
                                intent.putExtra(ChatActivity.EXTRA_SESSION_ID, sessionId);
                                startActivity(intent);
                                return;
                            }
                        }
                    }

                    if (linkUpOverlay.getVisibility() == View.VISIBLE) {
                        boolean anyPending = false;
                        for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots.getDocuments()) {
                            if ("pending".equals(doc.getString("status"))) {
                                anyPending = true;
                                break;
                            }
                        }
                        if (!anyPending) {
                            showSnackbar(getString(R.string.no_one_accepted));
                            hideOverlay();
                            setAvailability(true);
                        }
                    }
                });
    }

    private void cancelOtherPendingRequests(String myUid, String acceptedRequestId) {
        firestore.collection("linkupRequests")
                .whereEqualTo("fromUid", myUid)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(snapshots -> {
                    WriteBatch batch = firestore.batch();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots.getDocuments()) {
                        if (!doc.getId().equals(acceptedRequestId)) {
                            batch.update(doc.getReference(), "status", "expired");
                        }
                    }
                    if (!snapshots.isEmpty()) {
                        batch.commit();
                    }
                });
    }

    private void setAvailability(boolean available) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) return;
        firestore.collection("users").document(currentUser.getUid())
                .update("isAvailable", available);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (radiusAnimationRunnable != null) {
            timeoutHandler.removeCallbacks(radiusAnimationRunnable);
        }

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) return;
        String myUid = currentUser.getUid();
        firestore.collection("linkupRequests")
                .whereEqualTo("fromUid", myUid)
                .whereEqualTo("status", "pending")
                .get()
                .addOnSuccessListener(snapshots -> {
                    if (snapshots.isEmpty()) return;
                    WriteBatch batch = firestore.batch();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots.getDocuments()) {
                        batch.update(doc.getReference(), "status", "expired");
                    }
                    batch.update(firestore.collection("users").document(myUid), "isAvailable", false);
                    batch.commit();
                });
    }
}
