package com.kamron.pogoiv.pokeflycomponents.fractions;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;

import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;

import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.kamron.pogoiv.GoIVSettings;
import com.kamron.pogoiv.Pokefly;
import com.kamron.pogoiv.R;
import com.kamron.pogoiv.databinding.FractionInputBinding;
import com.kamron.pogoiv.scanlogic.Data;
import com.kamron.pogoiv.scanlogic.IVCombination;
import com.kamron.pogoiv.scanlogic.PokeInfoCalculator;
import com.kamron.pogoiv.scanlogic.Pokemon;
import com.kamron.pogoiv.scanlogic.PokemonBase;
import com.kamron.pogoiv.scanlogic.PokemonNameCorrector;
import com.kamron.pogoiv.scanlogic.ScanResult;
import com.kamron.pogoiv.utils.GUIColorFromPokeType;
import com.kamron.pogoiv.utils.LevelRange;
import com.kamron.pogoiv.utils.ReactiveColorListener;
import com.kamron.pogoiv.utils.fractions.Fraction;
import com.kamron.pogoiv.widgets.PokemonSpinnerAdapter;

import java.util.ArrayList;

import timber.log.Timber;

public class InputFraction extends Fraction implements ReactiveColorListener {

    private final Pokefly pokefly;
    private final PokeInfoCalculator pokeInfoCalculator;

    private PokemonSpinnerAdapter pokeInputAdapter;

    private FractionInputBinding binding;

    //since the fragment calls onchanged, ontextchanged etc methods on fragment creation, the
    //fragment will update and calculate the pokemon several times when the ui is created.
    //To prevent this, this boolean stops any calculation, until its set to true.
    private boolean isInitiated = false;

    public InputFraction(@NonNull Pokefly pokefly) {
        this.pokefly = pokefly;
        this.pokeInfoCalculator = PokeInfoCalculator.getInstance();
    }

    @Override
    public void onCreate(LayoutInflater inflater, ViewGroup parent, boolean attachToParent) {
        binding = FractionInputBinding.inflate(inflater, parent, attachToParent);

        // Initialize pokemon species spinner
        pokeInputAdapter = new PokemonSpinnerAdapter(pokefly, R.layout.spinner_pokemon, new ArrayList<>());
        binding.spnPokemonName.setAdapter(pokeInputAdapter);

        binding.spnPokemonName.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                updateIVInputFractionPreview();
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        // Fix arc when level is changed
        createArcAdjuster();

        // Setup manual pokemon species input
        initializePokemonAutoCompleteTextView();

        PokemonNameCorrector.PokeDist possiblePoke;
        // If not-null, then the Pokemon has been manually set, so we shouldn't guess it.
        if (Pokefly.scanData.getPokemon() != null) {
            possiblePoke = new PokemonNameCorrector.PokeDist(Pokefly.scanData.getPokemon(), 0);
        } else {
            // Guess the species
            possiblePoke = PokemonNameCorrector.getInstance(pokefly).getPossiblePokemon(Pokefly.scanData);
        }

        // set color based on similarity
        if (possiblePoke.dist == 0) {
            binding.spnPokemonName.setBackgroundColor(Color.parseColor("#FFF9F9F9"));
        } else if (possiblePoke.dist < 2) {
            binding.spnPokemonName.setBackgroundColor(Color.parseColor("#ffffdd"));
        } else {
            binding.spnPokemonName.setBackgroundColor(Color.parseColor("#ffdddd"));
        }

        resetToSpinner(); //always have the input as spinner as default

        binding.autoCompleteTextView1.setText("");
        pokeInputAdapter.updatePokemonList(
                pokeInfoCalculator.getEvolutionForms(possiblePoke.pokemon));
        int selection = pokeInputAdapter.getPosition(possiblePoke.pokemon);
        binding.spnPokemonName.setSelection(selection);

        binding.etHp.setText(optionalIntToString(Pokefly.scanData.getPokemonHP()));
        binding.etCp.setText(optionalIntToString(Pokefly.scanData.getPokemonCP()));
        binding.etCandy.setText(optionalIntToString(Pokefly.scanData.getPokemonCandyAmount()));

        adjustArcPointerBar(Pokefly.scanData.getEstimatedPokemonLevel().min);

        showCandyTextBoxBasedOnSettings();

        GUIColorFromPokeType.getInstance().setListenTo(this);
        updateGuiColors();
        isInitiated = true;
        updateIVInputFractionPreview();

        binding.pokePickerToggleSpinnerVsInput.setOnClickListener(view -> toggleSpinnerVsInput());

        TextWatcher textChangedListener = new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                updateIVFractionSpinnerDueToTextChange();
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        };
        binding.etCandy.addTextChangedListener(textChangedListener);
        binding.etHp.addTextChangedListener(textChangedListener);
        binding.etCandy.addTextChangedListener(textChangedListener);
        binding.autoCompleteTextView1.addTextChangedListener(textChangedListener);

        binding.btnDecrementLevel.setOnClickListener(view -> decrementLevel());
        binding.btnIncrementLevel.setOnClickListener(view -> incrementLevel());

        binding.btnCheckIv.setOnClickListener(view -> checkIv());
        binding.appraisalButton.setOnClickListener(view -> onAppraisal());
        binding.btnClose.setOnClickListener(view -> onClose());
    }

    @Override
    public void onDestroy() {
        saveToPokefly();
        GUIColorFromPokeType.getInstance().removeListener(this);
        binding = null;
    }

    @Override
    public Anchor getAnchor() {
        return Anchor.BOTTOM;
    }

    @Override
    public int getVerticalOffset(@NonNull DisplayMetrics displayMetrics) {
        return 0;
    }

    private void saveToPokefly() {
        if (isInitiated){
            final String hp = binding.etHp.getText().toString();
            if (!Strings.isNullOrEmpty(hp)) {
                try {
                    Pokefly.scanData.setPokemonHP(Integer.parseInt(hp));
                } catch (NumberFormatException e) {
                    Timber.d(e);
                }
            }
            final String cp = binding.etCp.getText().toString();
            if (!Strings.isNullOrEmpty(cp)) {
                try {
                    Pokefly.scanData.setPokemonCP(Integer.parseInt(cp));
                } catch (NumberFormatException e) {
                    Timber.d(e);
                }
            }
            final String candies = binding.etCandy.getText().toString();
            if (!Strings.isNullOrEmpty(candies)) {
                try {
                    Pokefly.scanData.setPokemonCandyAmount(Integer.parseInt(candies));
                } catch (NumberFormatException e) {
                    Timber.d(e);
                }
            }
            Pokemon pokemon = interpretWhichPokemonUserInput();
            if (pokemon != null) {
              Pokefly.scanData.setPokemon(pokemon);
            }
        }
    }

    /**
     * In the input screen, switches between the two methods the user has of picking pokemon - a dropdown list, or
     * typing.
     */
    public void toggleSpinnerVsInput() {
        if (binding.autoCompleteTextView1.getVisibility() == View.GONE) {
            binding.autoCompleteTextView1.setVisibility(View.VISIBLE);


            Bitmap icon = BitmapFactory.decodeResource(pokefly.getResources(),
                    R.drawable.toggleselectwrite);
            binding.pokePickerToggleSpinnerVsInput.setImageBitmap(icon);
            binding.autoCompleteTextView1.requestFocus();
            binding.spnPokemonName.setVisibility(View.GONE);
        } else {
            resetToSpinner();
            Bitmap icon = BitmapFactory.decodeResource(pokefly.getResources(),
                    R.drawable.toggleselectmenu);
            binding.pokePickerToggleSpinnerVsInput.setImageBitmap(icon);
        }

        updateGuiColors();
    }


    private void resetToSpinner() {
        binding.autoCompleteTextView1.setVisibility(View.GONE);
        binding.spnPokemonName.setVisibility(View.VISIBLE);
    }

    private void adjustArcPointerBar(double estimatedPokemonLevel) {
        pokefly.setArcPointer(estimatedPokemonLevel);
        binding.sbArcAdjust.setProgress(Data.levelToLevelIdx(estimatedPokemonLevel));
        updateIVInputFractionPreview();
    }

    public void updateIVFractionSpinnerDueToTextChange() {
        updateIVInputFractionPreview();
    }

    /**
     * Update the text on the 'next' button to indicate quick IV overview
     */
    private void updateIVInputFractionPreview() {
        if(isInitiated){
            saveToPokefly();

            updateIVPreview(pokefly, binding.btnCheckIv);
        }
    }

    public static void updateIVPreview(Pokefly pokefly, Button btnCheckIv) {
        ScanResult scanResult = null;
        try {
            scanResult = pokefly.computeIVWithoutUIChange();
        } catch (IllegalStateException e) {
            //Couldn't compute a valid scanresult. This is most likely due to missing HP / CP values
        }

        // If it couldn't compute a scan result it should behave the same as if there are no valid
        // IV combinations.
        int possibleIVs = scanResult == null ? 0 : scanResult.getIVCombinations().size();
        //btnCheckIv.setEnabled(possibleIVs != 0);
        if (possibleIVs == 0) {
            btnCheckIv.setText("? | More info");
        } else {
            if (possibleIVs == 1) {
                IVCombination result = scanResult.getIVCombinations().get(0);
                btnCheckIv.setText(result.percentPerfect + "% (" + result.att + ":" + result.def + ":" + result.sta + ") | More info");
            } else if (scanResult.getLowestIVCombination().percentPerfect == scanResult
                    .getHighestIVCombination().percentPerfect) {
                btnCheckIv.setText(scanResult.getLowestIVCombination().percentPerfect + "% | More info");
            } else {
                btnCheckIv.setText(scanResult.getLowestIVCombination().percentPerfect + "% - " + scanResult
                        .getHighestIVCombination().percentPerfect + "% | More info");
            }
        }
    }

    public void decrementLevel() {
        if (Pokefly.scanData.getEstimatedPokemonLevel().min > Data.MINIMUM_POKEMON_LEVEL) {
            Pokefly.scanData.getEstimatedPokemonLevel().dec();
            adjustArcPointerBar(Pokefly.scanData.getEstimatedPokemonLevel().min);
        }
    }

    public void incrementLevel() {
        if (Data.levelToLevelIdx(Pokefly.scanData.getEstimatedPokemonLevel().min) < binding.sbArcAdjust.getMax()) {
            Pokefly.scanData.getEstimatedPokemonLevel().inc();
            adjustArcPointerBar(Pokefly.scanData.getEstimatedPokemonLevel().min);
        }
    }

    /**
     * Creates the arc adjuster used to move the arc pointer in the scan screen.
     */
    private void createArcAdjuster() {
        // The max seek bar value will be the maximum wild pokemon level or the trainer max capture level if higher
        binding.sbArcAdjust.setMax(Math.max(Data.levelToLevelIdx(Data.MAXIMUM_WILD_POKEMON_LEVEL),
                Data.levelToLevelIdx(Data.trainerLevelToMaxPokeLevel(pokefly.getTrainerLevel()))));

        binding.sbArcAdjust.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    Pokefly.scanData.setEstimatedPokemonLevelRange(new LevelRange(Data.levelIdxToLevel(progress)));
                }
                pokefly.setArcPointer(Pokefly.scanData.getEstimatedPokemonLevel().min);
                binding.levelIndicator.setText(Pokefly.scanData.getEstimatedPokemonLevel().toString());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                binding.btnCheckIv.setText("...");
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                updateIVInputFractionPreview();
            }
        });
    }

    /**
     * Initialises the autocompletetextview which allows people to search for pokemon names.
     */
    private void initializePokemonAutoCompleteTextView() {
        String[] pokeList = pokeInfoCalculator.getPokemonNamesWithFormArray();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(pokefly, R.layout.autocomplete_pokemon_list_item, pokeList);
        binding.autoCompleteTextView1.setAdapter(adapter);
        binding.autoCompleteTextView1.setThreshold(1);
    }

    /**
     * showCandyTextBoxBasedOnSettings
     * Shows candy text box if pokespam is enabled
     * Will set the Text Edit box to use next action or done if its the last text box.
     */
    private void showCandyTextBoxBasedOnSettings() {
        //enable/disable visibility based on PokeSpam enabled or not
        if (GoIVSettings.getInstance(pokefly).isPokeSpamEnabled()) {
            binding.llPokeSpamSpace.setVisibility(View.VISIBLE);
            binding.llPokeSpamDialogInputContentBox.setVisibility(View.VISIBLE);
            binding.etHp.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        } else {
            binding.llPokeSpamSpace.setVisibility(View.GONE);
            binding.llPokeSpamDialogInputContentBox.setVisibility(View.GONE);
            binding.etHp.setImeOptions(EditorInfo.IME_ACTION_DONE);
        }
    }

    private <T> String optionalIntToString(Optional<T> src) {
        return src.transform(Object::toString).or("");
    }

    /**
     * Checks whether the user input a pokemon using the spinner or the text input on the input screen
     * null if no correct input was provided (user typed non-existant pokemon or spinner error)
     * If user typed in incorrect pokemon, a toast will be displayed.
     *
     * @return The pokemon the user selected/typed or null if user put wrong input
     */
    private Pokemon interpretWhichPokemonUserInput() {
        //below picks a pokemon from either the pokemon spinner or the user text input
        Pokemon pokemon = null;
        if (binding.spnPokemonName.getVisibility() == View.VISIBLE) { //user picked pokemon from spinner
            //This could be pokemon = binding.spnPokemonName.getSelectedItem(); if they didn't give it type Object.
            pokemon = pokeInputAdapter.getItem(binding.spnPokemonName.getSelectedItemPosition());
        } else { //user typed manually
            String userInput = binding.autoCompleteTextView1.getText().toString();
            int lowestDist = Integer.MAX_VALUE;
            for (PokemonBase poke : pokeInfoCalculator.getPokedex()) {
                int dist = Data.levenshteinDistance(poke.name, userInput);
                if (dist < lowestDist) {
                    lowestDist = dist;
                    pokemon = poke.forms.get(0);
                }
                // Even though the above might've used forms[0], iterate over all forms as the form's name might have
                // a better distance. There might be a case where forms[1] beats poke.name but not forms[0].
                for (Pokemon form : poke.forms) {
                    dist = Data.levenshteinDistance(form.name, userInput);
                    if (dist < lowestDist) {
                        lowestDist = dist;
                        pokemon = form;
                    }
                }
            }
            if (pokemon == null) { //no such pokemon was found, show error toast and abort showing results
                Toast.makeText(pokefly, userInput + pokefly.getString(R.string.wrong_pokemon_name_input),
                        Toast.LENGTH_SHORT).show();
                return null;
            }
        }
        return pokemon;
    }

    /**
     * Method called when user presses "check iv" in the input screen, which takes the user to the result screen.
     */
    void checkIv() {
        saveToPokefly();
        pokefly.computeIv();
    }

    void onAppraisal() {
        pokefly.navigateToAppraisalFraction();
    }

    void onClose() {
        pokefly.closeInfoDialog();
    }

    @Override
    public void updateGuiColors() {
        int c = GUIColorFromPokeType.getInstance().getColor();
        binding.inputHeaderBG.setBackgroundColor(c);
        binding.appraisalButton.setBackgroundColor(c);
        binding.etCp.setTextColor(c);
        binding.etHp.setTextColor(c);
        binding.etCandy.setTextColor(c);
        binding.btnCheckIv.setBackgroundColor(c);

        PorterDuff.Mode mMode = PorterDuff.Mode.SRC_ATOP;
        Drawable d = binding.pokePickerToggleSpinnerVsInput.getDrawable();
        d.setColorFilter(c,mMode);
    }
}
