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

	private final OsmandPreference<DiffElevationType> typePreference; // TODO: rename displayPreference display
	private final OsmandPreference<DiffElevationTarget> targetPreference;

	public DiffElevationToPointWidgetState(@NonNull OsmandApplication app, @Nullable String customId, boolean intermediate) {
		super(app);
		this.typePreference = registerTypePreference(customId);
		this.targetPreference = registerTargetPref(customId, intermediate ? DiffElevationTarget.NEXT_INTERMEDIATE : DiffElevationTarget.DESTINATION);
	}

	@NonNull
	public OsmandPreference<DiffElevationType> getPreference() {
		return typePreference;
	}

	@NonNull
	public OsmandPreference<DiffElevationTarget> getTargetPreference() {
		return targetPreference;
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
		//registerTimeTypePref(customId).setModeValue(appMode, arrivalTimeOrTimeToGo.getModeValue(sourceAppMode));
		registerTypePreference(customId).setModeValue(appMode, typePreference.getModeValue(sourceAppMode));
		registerTargetPref(customId, null).setModeValue(appMode, targetPreference.getModeValue(sourceAppMode));
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
	private OsmandPreference<DiffElevationTarget> registerTargetPref(@Nullable String customId, DiffElevationTarget init) {
		String prefId = "diff_elevation_target";
		if (!Algorithms.isEmpty(customId)) {
			prefId += customId;
		}
		if (init == null) init = DiffElevationTarget.DESTINATION;
		return settings.registerEnumStringPreference(prefId, init, DiffElevationTarget.values(), DiffElevationTarget.class).makeProfile();
	}

	public enum DiffElevationTarget {
		DESTINATION, NEXT_INTERMEDIATE, NEXT_STRETCH;
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