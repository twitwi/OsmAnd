package net.osmand.plus.helpers;

import android.graphics.Typeface;
import android.os.AsyncTask;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import net.osmand.GPXUtilities.Author;
import net.osmand.GPXUtilities.Copyright;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.GPXTrackAnalysis;
import net.osmand.GPXUtilities.Metadata;
import net.osmand.GPXUtilities.Track;
import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.GPXUtilities.WptPt;
import net.osmand.plus.ColorUtilities;
import net.osmand.plus.FilteredSelectedGpxFile;
import net.osmand.plus.GPXDatabase.GpxDataItem;
import net.osmand.plus.GpxSelectionHelper;
import net.osmand.plus.GpxSelectionHelper.GpxDisplayGroup;
import net.osmand.plus.GpxSelectionHelper.SelectedGpxFile;
import net.osmand.plus.OsmAndFormatter;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.util.MapUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

public class GpsFilterHelper {

	private final OsmandApplication app;
	private final Set<GpsFilterListener> listeners = new HashSet<>();

	private final Executor singleThreadExecutor = Executors.newSingleThreadExecutor();
	private GpsFilterTask gpsFilterTask = null;

	public GpsFilterHelper(@NonNull OsmandApplication app) {
		this.app = app;
	}

	public void addListener(@NonNull GpsFilterListener listener) {
		listeners.add(listener);
	}

	public void removeListener(@NonNull GpsFilterListener listener) {
		listeners.remove(listener);
	}

	public void clearListeners() {
		listeners.clear();
	}

	public void filterGpxFile(@NonNull FilteredSelectedGpxFile filteredSelectedGpxFile) {
		if (gpsFilterTask != null) {
			gpsFilterTask.cancel(false);
		}
		gpsFilterTask = new GpsFilterTask(app, filteredSelectedGpxFile, listeners);
		gpsFilterTask.executeOnExecutor(singleThreadExecutor);
	}

	@SuppressWarnings("deprecation")
	private static class GpsFilterTask extends AsyncTask<Void, Void, Boolean> {

		private final OsmandApplication app;
		private final FilteredSelectedGpxFile filteredSelectedGpxFile;
		private final Set<GpsFilterListener> listeners;

		private GPXFile filteredGpxFile;
		private GPXTrackAnalysis trackAnalysis;
		private List<GpxDisplayGroup> displayGroups;

		public GpsFilterTask(@NonNull OsmandApplication app,
		                     @NonNull FilteredSelectedGpxFile filteredGpx,
		                     @NonNull Set<GpsFilterListener> listeners) {
			this.app = app;
			this.filteredSelectedGpxFile = filteredGpx;
			this.listeners = listeners;
		}

		@Override
		protected Boolean doInBackground(Void... voids) {
			SelectedGpxFile sourceSelectedGpxFile = filteredSelectedGpxFile.getSourceSelectedGpxFile();
			GPXFile sourceGpx = sourceSelectedGpxFile.getGpxFile();

			filteredGpxFile = copyGpxFile(app, sourceGpx);
			filteredGpxFile.tracks.clear();

			int pointsCount = 0;
			for (Track track : sourceGpx.tracks) {

				Track filteredTrack = new Track();
				filteredTrack.name = track.name;
				filteredTrack.desc = track.desc;

				for (TrkSegment segment : track.segments) {

					if (segment.generalSegment) {
						continue;
					}

					TrkSegment filteredSegment = new TrkSegment();
					filteredSegment.name = segment.name;

					double cumulativeDistance = 0;
					WptPt previousPoint = null;
					List<WptPt> points = segment.points;

					for (int i = 0; i < points.size(); i++) {

						if (isCancelled()) {
							return false;
						}

						WptPt point = points.get(i);

						if (previousPoint != null) {
							cumulativeDistance += MapUtils.getDistance(previousPoint.lat, previousPoint.lon,
									point.lat, point.lon);
						}
						boolean firstOrLast = i == 0 || i + 1 == points.size();

						if (acceptPoint(point, pointsCount, cumulativeDistance, firstOrLast)) {
							filteredSegment.points.add(new WptPt(point));
							cumulativeDistance = 0;
						}

						pointsCount++;
						previousPoint = point;
					}

					if (filteredSegment.points.size() != 0) {
						filteredTrack.segments.add(filteredSegment);
					}
				}

				if (filteredTrack.segments.size() != 0) {
					filteredGpxFile.tracks.add(filteredTrack);
				}
			}

			if (filteredSelectedGpxFile.isJoinSegments()) {
				filteredGpxFile.addGeneralTrack();
			}

			trackAnalysis = filteredGpxFile.getAnalysis(System.currentTimeMillis());
			displayGroups = processSplit(filteredGpxFile);

			return true;
		}

		private boolean acceptPoint(@NonNull WptPt point, int pointIndex, double cumulativeDistance, boolean firstOrLast) {
			SpeedFilter speedFilter = filteredSelectedGpxFile.getSpeedFilter();
			AltitudeFilter altitudeFilter = filteredSelectedGpxFile.getAltitudeFilter();
			HdopFilter hdopFilter = filteredSelectedGpxFile.getHdopFilter();
			SmoothingFilter smoothingFilter = filteredSelectedGpxFile.getSmoothingFilter();

			return speedFilter.acceptPoint(point, pointIndex, cumulativeDistance)
					&& altitudeFilter.acceptPoint(point, pointIndex, cumulativeDistance)
					&& hdopFilter.acceptPoint(point, pointIndex, cumulativeDistance)
					&& (firstOrLast || smoothingFilter.acceptPoint(point, pointIndex, cumulativeDistance));
		}

		@Nullable
		private List<GpxDisplayGroup> processSplit(@NonNull GPXFile gpxFile) {
			List<GpxDataItem> dataItems = app.getGpxDbHelper().getSplitItems();
			for (GpxDataItem dataItem : dataItems) {
				if (dataItem.getFile().getAbsolutePath().equals(gpxFile.path)) {
					return GpxSelectionHelper.processSplit(app, dataItem, gpxFile);
				}
			}
			return null;
		}

		@Override
		protected void onPostExecute(@NonNull Boolean successfulFinish) {
			if (successfulFinish && !isCancelled()) {
				filteredSelectedGpxFile.updateGpxFile(filteredGpxFile);
				filteredSelectedGpxFile.setTrackAnalysis(trackAnalysis);
				if (displayGroups != null) {
					filteredSelectedGpxFile.setDisplayGroups(displayGroups);
				}
				for (GpsFilterListener listener : listeners) {
					listener.onFinishFiltering();
				}
			}
		}
	}

	@NonNull
	public static GPXFile copyGpxFile(@NonNull OsmandApplication app, @NonNull GPXFile source) {
		GPXFile copy = new GPXFile(Version.getFullVersion(app));
		copy.author = source.author;
		if (source.metadata != null) {
			copy.metadata = copyMetadata(source.metadata);
		}
		copy.tracks = copyTracks(source.tracks);
		copy.addPoints(source.getPoints());
		copy.routes = new ArrayList<>(source.routes);
		copy.path = source.path;
		copy.hasAltitude = source.hasAltitude;
		copy.modifiedTime = System.currentTimeMillis();
		copy.copyExtensions(source);
		return copy;
	}

	@NonNull
	private static List<Track> copyTracks(@NonNull List<Track> sourceTracks) {
		List<Track> copiedTracks = new ArrayList<>(sourceTracks.size());
		for (Track sourceTrack : sourceTracks) {

			Track trackCopy = new Track();
			trackCopy.name = sourceTrack.name;
			trackCopy.desc = sourceTrack.desc;
			trackCopy.generalTrack = sourceTrack.generalTrack;
			copiedTracks.add(trackCopy);

			for (TrkSegment sourceSegment : sourceTrack.segments) {

				TrkSegment segmentCopy = new TrkSegment();
				segmentCopy.name = sourceSegment.name;
				segmentCopy.generalSegment = sourceSegment.generalSegment;
				trackCopy.segments.add(segmentCopy);

				for (WptPt sourcePoint : sourceSegment.points) {
					segmentCopy.points.add(new WptPt(sourcePoint));
				}
			}
		}

		return copiedTracks;
	}

	@NonNull
	private static Metadata copyMetadata(@NonNull Metadata source) {
		Metadata copy = new Metadata();
		copy.name = source.name;
		copy.desc = source.desc;
		copy.link = source.link;
		copy.keywords = source.keywords;
		copy.time = source.time;

		if (source.author != null) {
			Author author = new Author();
			author.name = source.author.name;
			author.email = source.author.email;
			author.link = source.author.link;
			author.copyExtensions(source.author);
			copy.author = author;
		}

		if (source.copyright != null) {
			Copyright copyright = new Copyright();
			copyright.author = source.copyright.author;
			copyright.year = source.copyright.year;
			copyright.license = source.copyright.license;
			copyright.copyExtensions(source.copyright);
			copy.copyright = copyright;
		}

		copy.copyExtensions(source);

		return copy;
	}

	public static abstract class GpsFilter {

		protected static final int SPAN_FLAGS = Spanned.SPAN_EXCLUSIVE_INCLUSIVE;

		protected final GPXTrackAnalysis analysis;

		protected double selectedMinValue;
		protected double selectedMaxValue;

		protected boolean nightMode;

		protected final ForegroundColorSpan blackTextSpan;
		protected final ForegroundColorSpan greyTextSpan;
		protected final StyleSpan boldSpan;

		public GpsFilter(@NonNull OsmandApplication app, @NonNull SelectedGpxFile selectedGpxFile) {
			this.analysis = selectedGpxFile.getTrackAnalysis(app);

			this.selectedMaxValue = getMaxValue();
			if (isRangeSupported()) {
				this.selectedMinValue = getMinValue();
			}

			nightMode = app.getDaynightHelper().isNightModeForMapControls();

			blackTextSpan = new ForegroundColorSpan(ColorUtilities.getPrimaryTextColor(app, nightMode));
			greyTextSpan = new ForegroundColorSpan(ColorUtilities.getSecondaryTextColor(app, nightMode));
			boldSpan = new StyleSpan(Typeface.BOLD);
		}

		protected void checkSelectedValues() {
			if (isRangeSupported()) {
				if (selectedMinValue > selectedMaxValue) {
					double temp = selectedMinValue;
					selectedMinValue = selectedMaxValue;
					selectedMaxValue = temp;
				}
				if (selectedMinValue < getMinValue()) {
					selectedMinValue = getMinValue();
				}
			}
			if (selectedMaxValue > getMaxValue()) {
				selectedMaxValue = getMaxValue();
			}
		}

		public abstract boolean isNeeded();

		public abstract boolean isRangeSupported();

		public abstract boolean acceptPoint(@NonNull WptPt point, int pointIndex, double distanceToLastSurvivedPoint);

		public abstract double getMinValue();

		public abstract double getMaxValue();

		public void updateValue(double maxValue) {
			// Not implemented
		}

		public void updateValues(double minValue, double maxValue) {
			// Not implemented
		}

		public void reset() {
			if (isRangeSupported()) {
				updateValues(getMinValue(), getMaxValue());
			} else {
				updateValue(getMaxValue());
			}
		}

		public final double getSelectedMinValue() {
			return isRangeSupported() ? selectedMinValue : getMinValue();
		}

		public final double getSelectedMaxValue() {
			return selectedMaxValue;
		}

		@NonNull
		public CharSequence getFormattedStyledValue(@NonNull OsmandApplication app, double value) {
			String formattedValue = getFormattedValue(value, app);
			int spaceIndex = formattedValue.indexOf(" ");

			int endIndex = spaceIndex == -1 ? formattedValue.length() : spaceIndex;
			SpannableString spannableString = new SpannableString(formattedValue);
			spannableString.setSpan(blackTextSpan, 0, endIndex, SPAN_FLAGS);

			if (spaceIndex != -1) {
				spannableString.setSpan(greyTextSpan, spaceIndex + 1, formattedValue.length(), SPAN_FLAGS);
			}

			return spannableString;
		}

		@NonNull
		public abstract String getFormattedValue(double value, @NonNull OsmandApplication app);

		@NonNull
		public abstract CharSequence getFilterTitle(@NonNull OsmandApplication app);

		@NonNull
		protected CharSequence styleFilterTitle(@NonNull String title, int boldEndIndex) {
			SpannableString spannableTitle = new SpannableString(title);
			spannableTitle.setSpan(blackTextSpan, 0,boldEndIndex, SPAN_FLAGS);
			spannableTitle.setSpan(boldSpan, 0, boldEndIndex, SPAN_FLAGS);
			spannableTitle.setSpan(greyTextSpan, boldEndIndex, title.length(), SPAN_FLAGS);
			return spannableTitle;
		}

		@NonNull
		public abstract String getLeftText(@NonNull OsmandApplication app);

		@NonNull
		public abstract String getRightText(@NonNull OsmandApplication app);

		@StringRes
		public abstract int getDescriptionId();
	}

	public static class SmoothingFilter extends GpsFilter {

		public SmoothingFilter(@NonNull OsmandApplication app, @NonNull SelectedGpxFile selectedGpxFile) {
			super(app, selectedGpxFile);
			selectedMaxValue = getMinValue();
		}

		@Override
		public boolean isNeeded() {
			return true;
		}

		@Override
		public boolean isRangeSupported() {
			return false;
		}

		@Override
		public boolean acceptPoint(@NonNull WptPt point, int pointIndex, double distanceToLastSurvivedPoint) {
			return !isNeeded() || getSelectedMaxValue() == 0 || distanceToLastSurvivedPoint > getSelectedMaxValue();
		}

		@Override
		public double getMinValue() {
			return 0;
		}

		@Override
		public double getMaxValue() {
			return 100;
		}

		@Override
		public void updateValue(double maxValue) {
			selectedMaxValue = ((int) maxValue);
		}

		@Override
		public void reset() {
			updateValue(getMinValue());
		}

		@NonNull
		@Override
		public String getFormattedValue(double value, @NonNull OsmandApplication app) {
			return OsmAndFormatter.getFormattedDistance((float) value, app);
		}

		@NonNull
		@Override
		public CharSequence getFilterTitle(@NonNull OsmandApplication app) {
			String smoothing = app.getString(R.string.gps_filter_smoothing);
			String title = app.getString(R.string.ltr_or_rtl_combine_via_colon, smoothing,
					getFormattedValue(getSelectedMaxValue(), app));
			return styleFilterTitle(title, smoothing.length() + 1);
		}

		@NonNull
		@Override
		public String getLeftText(@NonNull OsmandApplication app) {
			return app.getString(R.string.distance_between_points);
		}

		@NonNull
		@Override
		public String getRightText(@NonNull OsmandApplication app) {
			return getFormattedValue(getSelectedMaxValue(), app);
		}

		@Override
		public int getDescriptionId() {
			return R.string.gps_filter_smoothing_desc;
		}
	}

	public static class SpeedFilter extends GpsFilter {

		public SpeedFilter(@NonNull OsmandApplication app, @NonNull SelectedGpxFile selectedGpxFile) {
			super(app, selectedGpxFile);
		}

		@Override
		public boolean isNeeded() {
			return analysis.isSpeedSpecified();
		}

		@Override
		public boolean isRangeSupported() {
			return true;
		}

		@Override
		public boolean acceptPoint(@NonNull WptPt point, int pointIndex, double distanceToLastSurvivedPoint) {
			float speed = analysis.speedData.get(pointIndex).speed;
			return !isNeeded() || getSelectedMinValue() <= speed && speed <= getSelectedMaxValue();
		}

		@Override
		public double getMinValue() {
			return 0d;
		}

		@Override
		public double getMaxValue() {
			return Math.ceil(analysis.maxSpeed);
		}

		@Override
		public void updateValues(double minValue, double maxValue) {
			selectedMinValue = minValue;
			selectedMaxValue = maxValue;
			checkSelectedValues();
		}

		@NonNull
		@Override
		public String getFormattedValue(double value, @NonNull OsmandApplication app) {
			return OsmAndFormatter.getFormattedSpeed((float) value, app, true);
		}

		@NonNull
		@Override
		public CharSequence getFilterTitle(@NonNull OsmandApplication app) {
			String speed = app.getString(R.string.map_widget_speed);
			String value;
			if (!isNeeded()) {
				value = app.getString(R.string.gpx_logging_no_data);
			} else {
				String min = OsmAndFormatter.getFormattedSpeed((float) getSelectedMinValue(), app, false);
				String max = OsmAndFormatter.getFormattedSpeed((float) getSelectedMaxValue(), app, false);
				String range = app.getString(R.string.ltr_or_rtl_combine_via_dash, min, max);
				String unit = app.getSettings().SPEED_SYSTEM.get().toShortString(app);
				value = app.getString(R.string.ltr_or_rtl_combine_via_space, range, unit);
			}
			String title = app.getString(R.string.ltr_or_rtl_combine_via_colon, speed, value);

			return styleFilterTitle(title, speed.length() + 1);
		}

		@NonNull
		@Override
		public String getLeftText(@NonNull OsmandApplication app) {
			return app.getString(R.string.shared_string_min);
		}

		@NonNull
		@Override
		public String getRightText(@NonNull OsmandApplication app) {
			return app.getString(R.string.shared_string_max);
		}

		@Override
		public int getDescriptionId() {
			return R.string.gps_filter_speed_altitude_desc;
		}
	}

	public static class AltitudeFilter extends GpsFilter {

		public AltitudeFilter(@NonNull OsmandApplication app, @NonNull SelectedGpxFile selectedGpxFile) {
			super(app, selectedGpxFile);
		}

		@Override
		public boolean isNeeded() {
			return analysis.isElevationSpecified();
		}

		@Override
		public boolean isRangeSupported() {
			return true;
		}

		@Override
		public boolean acceptPoint(@NonNull WptPt point, int pointIndex, double distanceToLastSurvivedPoint) {
			float altitude = analysis.elevationData.get(pointIndex).elevation;
			return !isNeeded() || getSelectedMinValue() <= altitude && altitude <= getSelectedMaxValue();
		}

		@Override
		public double getMinValue() {
			return ((int) Math.floor(analysis.minElevation));
		}

		@Override
		public double getMaxValue() {
			return ((int) Math.ceil(analysis.maxElevation));
		}

		@Override
		public void updateValues(double minValue, double maxValue) {
			selectedMinValue = ((int) minValue);
			selectedMaxValue = ((int) maxValue);
			checkSelectedValues();
		}

		@NonNull
		@Override
		public String getFormattedValue(double value, @NonNull OsmandApplication app) {
			return OsmAndFormatter.getFormattedAlt(value, app);
		}

		@NonNull
		@Override
		public CharSequence getFilterTitle(@NonNull OsmandApplication app) {
			String altitude = app.getString(R.string.altitude);
			String value;
			if (!isNeeded()) {
				value = app.getString(R.string.gpx_logging_no_data);
			} else {
				String minAltitude = getFormattedValue(getSelectedMinValue(), app);
				String maxAltitude = getFormattedValue(getSelectedMaxValue(), app);
				value = app.getString(R.string.ltr_or_rtl_combine_via_dash, minAltitude, maxAltitude);
			}
			String title = app.getString(R.string.ltr_or_rtl_combine_via_colon, altitude, value);

			return styleFilterTitle(title, altitude.length());
		}

		@NonNull
		@Override
		public String getLeftText(@NonNull OsmandApplication app) {
			return app.getString(R.string.shared_string_min);
		}

		@NonNull
		@Override
		public String getRightText(@NonNull OsmandApplication app) {
			return app.getString(R.string.shared_string_max);
		}

		@Override
		public int getDescriptionId() {
			return R.string.gps_filter_speed_altitude_desc;
		}
	}

	public static class HdopFilter extends GpsFilter {

		public HdopFilter(@NonNull OsmandApplication app, @NonNull SelectedGpxFile selectedGpxFile) {
			super(app, selectedGpxFile);
		}

		@Override
		public boolean isNeeded() {
			return analysis.isHdopSpecified();
		}

		@Override
		public boolean isRangeSupported() {
			return false;
		}

		@Override
		public boolean acceptPoint(@NonNull WptPt point, int pointIndex, double distanceToLastSurvivedPoint) {
			return !isNeeded() || point.hdop <= getSelectedMaxValue();
		}

		@Override
		public double getMinValue() {
			return 0d;
		}

		@Override
		public double getMaxValue() {
			return analysis.maxHdop;
		}

		@Override
		public void updateValue(double maxValue) {
			selectedMaxValue = maxValue;
		}

		@NonNull
		@Override
		public String getFormattedValue(double value, @NonNull OsmandApplication app) {
			return String.format(app.getLocaleHelper().getPreferredLocale(), "%.1f", value);
		}

		@NonNull
		@Override
		public CharSequence getFilterTitle(@NonNull OsmandApplication app) {
			String gpsPrecision = app.getString(R.string.gps_filter_precision);
			String value = isNeeded()
					? getFormattedValue(getSelectedMaxValue(), app)
					: app.getString(R.string.gpx_logging_no_data);
			String title = app.getString(R.string.ltr_or_rtl_combine_via_colon, gpsPrecision, value);
			return styleFilterTitle(title, gpsPrecision.length() + 1);
		}

		@NonNull
		@Override
		public String getLeftText(@NonNull OsmandApplication app) {
			return app.getString(R.string.max_hdop);
		}

		@NonNull
		@Override
		public String getRightText(@NonNull OsmandApplication app) {
			return getFormattedValue(getSelectedMaxValue(), app);
		}

		@Override
		public int getDescriptionId() {
			return R.string.gps_filter_hdop_desc;
		}
	}

	public interface GpsFilterListener {

		void onFinishFiltering();
	}
}