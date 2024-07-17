package net.osmand.plus.views.mapwidgets.widgetstates;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.settings.backend.ApplicationMode;
import net.osmand.plus.settings.backend.preferences.OsmandPreference;
import net.osmand.util.Algorithms;

import java.util.HashMap;
import java.util.Map;

public class DiffElevationToPointWidgetState extends WidgetState {

	private final OsmandPreference<DiffElevationDisplay> displayPreference;
	private final OsmandPreference<DiffElevationTarget> targetPreference;

	public DiffElevationToPointWidgetState(@NonNull OsmandApplication app, @Nullable String customId, DiffElevationTarget target) {
		super(app);
		this.displayPreference = registerTypePreference(customId);
		this.targetPreference = registerTargetPref(customId, target);
	}

	@NonNull
	public DiffElevationDisplay getDiffElevationDisplay() {
		return displayPreference.get();
	}

	@NonNull
	public DiffElevationTarget getDiffElevationTarget() {
		return targetPreference.get();
	}

	private Info getInfo() {
		return infos.get(getDiffElevationDisplay()).get(getDiffElevationTarget());
	}
	@NonNull
	@Override
	public String getTitle() {
		/* TODO?
		int timeToId = R.string.map_widget_time_to_intermediate;
		String timeToString = app.getString(timeToId);
		String stateTitle = app.getString(titleId);
		return context.getString(R.string.ltr_or_rtl_combine_via_colon, timeToString, stateTitle);
		*/
		return app.getString(getInfo().titleId);
	}

	@Override
	public int getSettingsIconId(boolean nightMode) {
		Info info = getInfo();
		return nightMode ? info.nightIconId : info.dayIconId;
	}

	@Override
	public void changeToNextState() {
		displayPreference.set(getDiffElevationDisplay().next());
	}

	@Override
	public void copyPrefs(@NonNull ApplicationMode appMode, @Nullable String customId) {
		copyPrefsFromMode(appMode, appMode, customId);
	}

	@Override
	public void copyPrefsFromMode(@NonNull ApplicationMode sourceAppMode, @NonNull ApplicationMode appMode, @Nullable String customId){
		registerTypePreference(customId).setModeValue(appMode, displayPreference.getModeValue(sourceAppMode));
		registerTargetPref(customId, null).setModeValue(appMode, targetPreference.getModeValue(sourceAppMode));
	}

	@NonNull
	private OsmandPreference<DiffElevationDisplay> registerTypePreference(@Nullable String customId) {
		String prefId = "diff_elevation_type";
		if (!Algorithms.isEmpty(customId)) {
			prefId += customId;
		}
		return settings.registerEnumStringPreference(prefId, DiffElevationDisplay.POSITIVE_DIFF, DiffElevationDisplay.values(), DiffElevationDisplay.class).makeProfile();
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

	public static class Info {
		public final int titleId;
		public final int dayIconId;
		public final int nightIconId;

		public Info(@StringRes int titleId,
					@DrawableRes int dayIconId,
					@DrawableRes int nightIconId) {
			this.titleId = titleId;
			this.dayIconId = dayIconId;
			this.nightIconId = nightIconId;
		}
	}
	private static final Map<DiffElevationDisplay, Map<DiffElevationTarget, Info>> infos = new HashMap<DiffElevationDisplay, Map<DiffElevationTarget, Info>>() {{
		// TODO: could distinguish titles?
		for (DiffElevationDisplay d: DiffElevationDisplay.values()) {
			this.put(d, new HashMap<DiffElevationTarget, Info>() {{
				this.put(DiffElevationTarget.DESTINATION, new Info(R.string.map_widget_diff_elevation, R.drawable.widget_destination_diff_elevation_day, R.drawable.widget_destination_diff_elevation_night));
				this.put(DiffElevationTarget.NEXT_INTERMEDIATE, new Info(R.string.map_widget_diff_elevation, R.drawable.widget_intermediate_diff_elevation_day, R.drawable.widget_intermediate_diff_elevation_night));
				this.put(DiffElevationTarget.NEXT_STRETCH, new Info(R.string.map_widget_diff_elevation, R.drawable.widget_next_stretch_diff_elevation_day, R.drawable.widget_next_stretch_diff_elevation_night));
			}});
		}
	}};


	public enum DiffElevationTarget {
		DESTINATION, NEXT_INTERMEDIATE, NEXT_STRETCH;
	}

	public enum DiffElevationDisplay {
		POSITIVE_DIFF, NEGATIVE_DIFF, BOTH_DIFF;

		@NonNull
		public DiffElevationDisplay next() {
			int nextItemIndex = (ordinal() + 1) % values().length;
			return values()[nextItemIndex];
		}
	}

}