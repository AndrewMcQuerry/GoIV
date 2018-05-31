package com.kamron.pogoiv.pokeflycomponents.fractions;

import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import com.kamron.pogoiv.Pokefly;
import com.kamron.pogoiv.R;
import com.kamron.pogoiv.pokeflycomponents.MoveInfoOnlineFetcher;
import com.kamron.pogoiv.scanlogic.IVScanResult;
import com.kamron.pogoiv.scanlogic.MovesetData;
import com.kamron.pogoiv.scanlogic.PokemonShareHandler;
import com.kamron.pogoiv.scanlogic.ScanContainer;
import com.kamron.pogoiv.utils.fractions.Fraction;
import com.kamron.pogoiv.widgets.PowerTableDataAdapter;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.codecrafters.tableview.SortableTableView;
import de.codecrafters.tableview.model.TableColumnWeightModel;
import de.codecrafters.tableview.toolkit.SimpleTableHeaderAdapter;


public class MovesetFraction extends Fraction {

    @BindView(R.id.sortableTable)
    SortableTableView sortableTable;
    @BindView(R.id.movesetConstrainLayout)
    ConstraintLayout movesetConstrainLayout;

    Pokefly pokefly;
    private List<MovesetData> movesets = new ArrayList();
    private IVScanResult ivScanResult;

    public MovesetFraction(@NonNull Pokefly pokefly, @NonNull IVScanResult ivScanResult) {
        this.pokefly = pokefly;
        this.ivScanResult = ivScanResult;
    }

    @Override public int getLayoutResId() {
        return R.layout.fraction_moveset;
    }

    @Override public void onCreate(@NonNull View rootView) {
        ButterKnife.bind(this, rootView);
        loadMovesetData();
        if (movesets.size() <= 0){
            createDummyData();
        }


        setupTableHeader();
        setupDataSorting();
        addDataToTable();
        //fixTableConstrainLayoutHeight();
        sortableTable.sort(2); //default to sorting column index 3 (atk)

    }

    /**
     * For some reason "wrap content" makes the constraintview more than 100DP too long, so here's a method to set it
     * manually.
     */
    private void fixTableConstrainLayoutHeight() {
        float DP = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, pokefly.getResources()
                .getDisplayMetrics());

        ViewGroup.LayoutParams params = movesetConstrainLayout.getLayoutParams();
        //constant length for header + length for every data row.
        params.height = (int) ((DP * 100) + (DP * movesets.size() * 10));
        movesetConstrainLayout.setLayoutParams(params);


        ViewGroup.LayoutParams params2 = sortableTable.getLayoutParams();
        //constant length for header + length for every data row.
        params2.height = (int) ((DP * 50) + (DP * movesets.size() * 20));
        sortableTable.setLayoutParams(params2);
    }

    /**
     * Adds comparators to the columns for attack and defence values.
     */
    private void setupDataSorting() {
        sortableTable.setColumnComparator(2, new MovesetData.AtkComparator());
        sortableTable.setColumnComparator(3, new MovesetData.DefComparator());
    }

    private void setupTableHeader() {

        String[] TABLE_HEADERS = {"Quick", "Charge", "Atk", "Def"};
        sortableTable.setHeaderAdapter(new SimpleTableHeaderAdapter(pokefly, TABLE_HEADERS));


        TableColumnWeightModel columnModel = new TableColumnWeightModel(4);
        columnModel.setColumnWeight(0, 2);
        columnModel.setColumnWeight(1, 2);
        columnModel.setColumnWeight(2, 1);
        columnModel.setColumnWeight(3, 1);
        sortableTable.setColumnModel(columnModel);
    }

    /**
     * Adds the data from the moveset list to the table.
     */
    private void addDataToTable() {

        MovesetData[] dataToShow = new MovesetData[movesets.size()];
        for (int i = 0; i < movesets.size(); i++) {
            dataToShow[i] = movesets.get(i);
        }
        sortableTable.setDataAdapter(new PowerTableDataAdapter(pokefly, dataToShow));
    }


    private void loadMovesetData() {
        MoveInfoOnlineFetcher onlineFetcher = new MoveInfoOnlineFetcher();

        movesets = onlineFetcher.getMovesetData(ivScanResult);
        //todo - not implemented : add the moveset data to the "moveset" list object.
    }

    /**
     * Adds dummy moveset objects to the moveset list. The data is not accurate. The movesets are from gyarados, which
     * is an edgecase for amount of available movesets. But the numbers for atk / defence are fake.
     */
    private void createDummyData() {
        movesets.add(new MovesetData("Waterfall", "Hydro pump", false, false, 11.0, 10.8, "water", "water"
        ));
        movesets.add(new MovesetData("Bite", "Hydro pump", false, false, 10.8, 5.0, "dark",
                "water"));
        movesets.add(new MovesetData("Bite", "Crunch", false, false, 10.0, 6.2, "dark", "water"));
        movesets.add(new MovesetData("Dragon tail", "Outrage", true, false, 9.8, 7.0, "dragon", "dragon"));
        movesets.add(
                new MovesetData("Dragon tail", "Hydro pump", true, false, 9.7, 9.2, "dragon", "water"));
        movesets.add(
                new MovesetData("Dragon Breath", "Hydro pump", true, false, 9.5, 10.8, "dragon", "water"));
        movesets.add(new MovesetData("Waterfall", "Crunch", false, false, 9.3, 6.2, "water", "water"));
        movesets.add(new MovesetData("Waterfall", "Outrage", false, false, 9.2, 7.2, "water", "dragon"));
        movesets.add(new MovesetData("Dragon tail", "Crunch", true, false, 9.0, 6.6, "dragon", "water"));
        movesets.add(new MovesetData("Dragon Breath", "Dragon Pulse", true, true, 8.8, 10.2, "dragon",
                "dragon"));
        movesets.add(new MovesetData("Bite", "Outrage", false, false, 8.6, 10.1, "dark", "dragon"));
        movesets.add(new MovesetData("Bite", "Dragon Pulse", false, true, 8.2, 8.6, "dark", "dragon"));
        movesets.add(new MovesetData("Bite", "Twister", false, true, 8.0, 7.3, "dark", "dragon"));
        movesets.add(
                new MovesetData("Dragon Breath", "Twister", true, true, 7.4, 4.2, "dragon", "dragon"));
        movesets.add(new MovesetData("Waterfall", "Dragon Pulse", false, true, 7.0, 7.2, "water", "dragon"));
        movesets.add(new MovesetData("Waterfall", "Twister", false, true, 6.5, 6.4, "water", "dragon"));
        movesets.add(
                new MovesetData("Dragon tail", "Dragon Pulse", true, true, 6.1, 5.5, "dragon", "dragon"));
        movesets.add(new MovesetData("Dragon tail", "Twister", true, true, 6.0, 6.7, "dragon", "dragon"));
        movesets.add(
                new MovesetData("Dragon Breath", "Outrage", true, false, 5.6, 6.8, "dragon", "dragon"));
        movesets.add(new MovesetData("Dragon Breath", "Crunch", true, false, 5.2, 7.2,
                "dragon", "dark"));
    }

    @Override public void onDestroy() {

    }

    @OnClick(R.id.powerUpButton)
    void onPowerUp() {
        pokefly.navigateToPowerUpFraction();
    }

    @OnClick(R.id.ivButton)
    void onMoveset() {
        pokefly.navigateToIVResultFraction();
    }

    @OnClick(R.id.btnBack)
    void onBack() {
        pokefly.navigateToInputFraction();
    }

    @OnClick(R.id.btnClose)
    void onClose() {
        pokefly.closeInfoDialog();
    }

    /**
     * Creates an intent to share the result of the pokemon scan, and closes the overlay.
     */
    @OnClick({R.id.shareWithOtherApp})
    void shareScannedPokemonInformation() {
        PokemonShareHandler communicator = new PokemonShareHandler();
        communicator.spreadResultIntent(pokefly, ScanContainer.scanContainer.currScan, pokefly.pokemonUniqueID);
        pokefly.closeInfoDialog();
    }
}
