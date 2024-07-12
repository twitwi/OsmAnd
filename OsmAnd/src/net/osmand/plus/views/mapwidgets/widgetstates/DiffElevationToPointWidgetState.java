package net.osmand.plus.views.mapwidgets.widgetstates;

import android.content.Context;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.util.Algorithms;

public class DiffElevationToPointWidgetState extends WidgetState {

	public static final int TIME_CONTROL_WIDGET_STATE_ARRIVAL_TIME = R.id.time_control_widget_state_arrival_time;
	public static final int TIME_CONTROL_WIDGET_STATE_TIME_TO_GO = R.id.time_control_widget_state_time_to_go;

	private final boolean intermediate;
	private final OsmandPreference<Boolean> arrivalTimeOrTimeToGo;
	private final OsmandPreference<DiffElevationType> typePreference;

	public DiffElevationToPointWidgetState(@NonNull OsmandApplication app, @Nullable String customId, boolean intermediate) {
		super(app);
		this.intermediate = intermediate;
		this.typePreference = registerTypePreference(customId);
		this.arrivalTimeOrTimeToGo = registerTimeTypePref(customId);
	}

	public boolean isIntermediate() {
		return intermediate;
	}

	@NonNull
	public OsmandPreference<Boolean> getPreference() {
		return arrivalTimeOrTimeToGo;
	}

	@NonNull
	public DiffElevationType getDiffElevationType() {
		return typePreference.get();
	}

	@NonNull
	@Override
	public String getTitle() {
		return getDiffElevationType().getTitle(app);
	}

	@Override
	public int getSettingsIconId(boolean nightMode) {
		return nightMode ? getDiffElevationType().nightIconId : getDiffElevationType().dayIconId;
	}

	@Override
	public void changeToNextState() {
		typePreference.set(getDiffElevationType().next());
	}

	@Override
	public void copyPrefs(@NonNull ApplicationMode appMode, @Nullable String customId) {
		copyPrefsFromMode(appMode, appMode, customId);
	}

	@Override
	public void copyPrefsFromMode(@NonNull ApplicationMode sourceAppMode, @NonNull ApplicationMode appMode, @Nullable String customId){
		registerTimeTypePref(customId).setModeValue(appMode, arrivalTimeOrTimeToGo.getModeValue(sourceAppMode));
	}

	@NonNull
	private OsmandPreference<DiffElevationType> registerTypePreference(@Nullable String customId) {
		String prefId = "diff_elevation_type";
		if (!Algorithms.isEmpty(customId)) {
			prefId += customId;
		}
		return settings.registerEnumStringPreference(prefId, DiffElevationType.POSITIVE_DIFF, DiffElevationType.values(), DiffElevationType.class).makeProfile();
	}
	@NonNull
	private OsmandPreference<Boolean> registerTimeTypePref(@Nullable String customId) {
		String prefId = intermediate ? "show_arrival_time" : "show_intermediate_arrival_time";
		if (!Algorithms.isEmpty(customId)) {
			prefId += customId;
		}
		return settings.registerBooleanPreference(prefId, true).makeProfile();
	}

	public enum DiffElevationType {
		POSITIVE_DIFF(R.string.map_widget_time, R.drawable.widget_destination_diff_elevation_day, R.drawable.widget_destination_diff_elevation_night),
		NEGATIVE_DIFF(R.string.access_arrival_time, R.drawable.widget_destination_diff_elevation_day, R.drawable.widget_destination_diff_elevation_night),
		BOTH_DIFF(R.string.map_widget_time, R.drawable.widget_destination_diff_elevation_day, R.drawable.widget_destination_diff_elevation_night);

		@StringRes
		public final int titleId;
		@DrawableRes
		public final int dayIconId;
		@DrawableRes
		public final int nightIconId;
		//public final boolean intermediate;

		DiffElevationType(@StringRes int titleId,
						@DrawableRes int dayIconId,
						@DrawableRes int nightIconId
						/*boolean intermediate*/) {
			this.titleId = titleId;
			this.dayIconId = dayIconId;
			this.nightIconId = nightIconId;
			//this.intermediate = intermediate;
		}

		@NonNull
		public String getTitle(@NonNull Context context) {
			int timeToId = R.string.map_widget_time_to_intermediate;
			String timeToString = context.getString(timeToId);
			String stateTitle = context.getString(titleId);
			return context.getString(R.string.ltr_or_rtl_combine_via_colon, timeToString, stateTitle);
		}

		@NonNull
		public DiffElevationType next() {
			int nextItemIndex = (ordinal() + 1) % values().length;
			return values()[nextItemIndex];
		}
	}

}