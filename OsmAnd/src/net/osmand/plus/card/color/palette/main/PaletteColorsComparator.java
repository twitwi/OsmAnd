package net.osmand.plus.card.color.palette.main;

import androidx.annotation.NonNull;

import net.osmand.plus.card.color.palette.migration.v1.data.PaletteColorV1;
import net.osmand.plus.card.color.palette.main.data.PaletteSortingMode;

import java.util.Comparator;

public class PaletteColorsComparator implements Comparator<PaletteColorV1> { // todo remove

	private final PaletteSortingMode sortingMode;

	public PaletteColorsComparator(@NonNull PaletteSortingMode sortingMode) {
		this.sortingMode = sortingMode;
	}

	@Override
	public int compare(PaletteColorV1 o1, PaletteColorV1 o2) {
		if (sortingMode == PaletteSortingMode.LAST_USED_TIME) {
			return Long.compare(o2.getLastUsedTime(), o1.getLastUsedTime());
		}
		// Otherwise, use original order
		return 0;
	}

}
