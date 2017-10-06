package com.tylersuehr.library;

import android.content.Context;
import android.graphics.PorterDuff;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.tylersuehr.library.data.Chip;
import com.tylersuehr.library.data.ChipDataSource;
import com.tylersuehr.library.data.ChipDataSourceManager;
import com.tylersuehr.library.data.ChipDataSourceObserver;

import java.util.ArrayList;
import java.util.List;

/**
 * Copyright © 2017 Tyler Suehr
 *
 * @author Tyler Suehr
 * @version 1.0
 */
class FilterableChipsAdapter extends RecyclerView.Adapter<FilterableChipsAdapter.Holder> implements Filterable {
    private static LetterTileProvider tileProvider;
    private final ChipDataSource dataSource;
    private final ChipOptions chipOptions;
    private final OnFilteredChipClickListener listener;
    private ChipFilter filter;


    FilterableChipsAdapter(Context c,
                           OnFilteredChipClickListener listener,
                           ChipOptions op,
                           ChipDataSource dataSource,
                           ChipDataSourceManager dataSourceManager) {
        this.listener = listener;
        this.dataSource = dataSource;
        this.chipOptions = op;

        tileProvider = getTileProvider(c);

        // Create a listener for the chips
        dataSourceManager.registerObserver(new ChipDataSourceObserver() {
            @Override
            public void onChipAdded(List<Chip> list, Chip addedChip) {
                notifyDataSetChanged();
            }

            @Override
            public void onChipRemoved(List<Chip> list, Chip removedChip) {
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public int getItemCount() {
        return dataSource.getFilteredChips().size();
    }

    @Override
    public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View v = inflater.inflate(R.layout.adapter_filtereable_item, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(Holder holder, int position) {
        final Chip chip = dataSource.getFilteredChips().get(position);

        // Set the chip avatar, if possible
        if (chipOptions.hasAvatarIcon && chip.getAvatarUri() != null) {
            holder.image.setVisibility(View.VISIBLE);
            holder.image.setImageURI(chip.getAvatarUri());
        } else if (chipOptions.hasAvatarIcon && chip.getAvatarDrawable() != null) {
            holder.image.setVisibility(View.VISIBLE);
            holder.image.setImageDrawable(chip.getAvatarDrawable());
        } else if (chipOptions.hasAvatarIcon) {
            holder.image.setVisibility(View.VISIBLE);
            holder.image.setImageBitmap(tileProvider.getLetterTile(chip.getTitle()));
        } else {
            holder.image.setVisibility(View.GONE);
        }

        // Set the chip title
        holder.title.setText(chip.getTitle());

        // Set the chip subtitle, if possible
        if (chip.getSubtitle() != null) {
            holder.subtitle.setVisibility(View.VISIBLE);
            holder.subtitle.setText(chip.getSubtitle());
        } else {
            holder.subtitle.setVisibility(View.GONE);
        }

        // Set chip colors from options, if possible
        if (chipOptions.filterableListBackgroundColor != null) {
            holder.itemView.getBackground().setColorFilter(chipOptions.filterableListBackgroundColor.getDefaultColor(), PorterDuff.Mode.SRC_ATOP);
        }
        if (chipOptions.filterableListTextColor != null) {
            holder.title.setTextColor(chipOptions.filterableListTextColor);
            holder.subtitle.setText(Utils.alpha(chipOptions.filterableListTextColor.getDefaultColor(), 150));
        }
    }

    @Override
    public Filter getFilter() {
        if (filter == null) {
            this.filter = new ChipFilter();
        }
        return filter;
    }

    private static LetterTileProvider getTileProvider(Context c) {
        if (tileProvider == null) {
            tileProvider = new LetterTileProvider(c);
        }
        return tileProvider;
    }


    class Holder extends RecyclerView.ViewHolder implements View.OnClickListener {
        CircleImageView image;
        TextView title;
        TextView subtitle;

        Holder(View v) {
            super(v);
            v.setOnClickListener(this);
            this.image = v.findViewById(R.id.image);
            this.title = v.findViewById(R.id.title);
            this.subtitle = v.findViewById(R.id.subtitle);
        }

        @Override
        public void onClick(View v) {
            // TODO: POSSIBLE OPTIMIZATION
            // Add some convenience methods to ChipDataSource to take and replace filterable chips
            // using indexes instead of the Chip model
            final Chip chip = dataSource.getFilteredChips().get(getAdapterPosition());
            dataSource.takeChip(chip);

            listener.onFilteredChipClick(chip);
        }
    }


    /**
     * Callbacks for filtered chip click events.
     */
    interface OnFilteredChipClickListener {
        void onFilteredChipClick(Chip chip);
    }


    /**
     * Concrete implementation of {@link Filter} to help us filter our list of filterable chips.
     *
     * This works by cloning the list of filterable chips, so that the original filterable chips
     * list is retained, and then inclusively filtering the data source filterable chips list.
     *
     * Once the data source filterable chips list is filtered, the adapter will notify data
     * set changes have happened.
     *
     * If the user removes the filter (removing all the typed characters), the original list
     * of filterable chips will be added back into the data source filterable chips.
     */
    private final class ChipFilter extends Filter {
        private final List<Chip> originalFiltered;


        // TODO: POSSIBLE OPTIMIZATION
        // Use the original chip list in ChipDataSource instead of this instantiated list in constructor
        // to store original filtered list (may have been the original idea to begin with)
        private ChipFilter() {
            this.originalFiltered = new ArrayList<>();
            this.originalFiltered.addAll(dataSource.getFilteredChips());
        }

        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults results = new FilterResults();

            dataSource.getFilteredChips().clear();
            if (TextUtils.isEmpty(constraint)) {
                dataSource.getFilteredChips().addAll(originalFiltered);
            } else {
                final String pattern = constraint.toString().toLowerCase().trim();
                for (Chip chip : originalFiltered) {
                    if (chip.getTitle().toLowerCase().contains(pattern)
                            || (chip.getSubtitle() != null && chip.getSubtitle().toLowerCase().replaceAll("\\s", "").contains(pattern))) {
                        dataSource.getFilteredChips().add(chip);
                    }
                }
            }

            results.values = dataSource.getFilteredChips();
            results.count = dataSource.getFilteredChips().size();
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            notifyDataSetChanged();
        }
    }
}