// prevent resubmit warning
if (window.history && window.history.replaceState && typeof window.history.replaceState === 'function') {
  window.history.replaceState(null, null, window.location.href);
}

document.addEventListener('DOMContentLoaded', function(event) {

  // handle back click
  var backLink = document.querySelector('.govuk-back-link');
  if (backLink !== null) {
    backLink.addEventListener('click', function(e){
      e.preventDefault();
      e.stopPropagation();
      window.history.back();
    });
  }
});

// Find first ancestor of el with tagName
// or undefined if not found
function upTo(el, tagName) {
  tagName = tagName.toLowerCase();

  while (el && el.parentNode) {
    el = el.parentNode;
    if (el.tagName && el.tagName.toLowerCase() == tagName) {
      return el;
    }
  }

  // Many DOM methods return null if they don't
  // find the element they are searching for
  // It would be OK to omit the following and just
  // return undefined
  return null;
}

if (typeof HMRCAccessibleAutocomplete != 'undefined' && document.querySelector('[date-module="hmrc-accessible-autocomplete"]') != null) {
  // load autocomplete
  var selectEl = document.querySelector('[date-module="hmrc-accessible-autocomplete"]');
  if (selectEl && selectEl.style.display !== "none") {
    HMRCAccessibleAutocomplete.enhanceSelectElement({
      id: selectEl.id, // Important that id is the same
      defaultValue: "",
      selectElement: selectEl
    });
  }

  // =====================================================
  // Polyfill autocomplete once loaded
  // =====================================================
  var checkForLoad = setInterval(checkForAutocompleteLoad, 500);
  var originalSelect = document.querySelector('[date-module="hmrc-accessible-autocomplete"]');
  var parentForm = upTo(originalSelect, 'form');

  function polyfillAutocomplete(){
    var combo = parentForm.querySelector('[role="combobox"]');
    // =====================================================
    // Update autocomplete once loaded with fallback's aria attributes
    // Ensures hint and error are read out before usage instructions
    // =====================================================
    if(originalSelect && originalSelect.getAttribute('aria-describedby') > ""){
      if(parentForm){
        if(combo){
          combo.setAttribute('aria-describedby', originalSelect.getAttribute('aria-describedby') + ' ' + combo.getAttribute('aria-describedby'));
        }
      }
    }

    // =====================================================
    // Ensure when user replaces valid answer with a non-valid answer, then valid answer is not retained
    // =====================================================
    var holdSubmit = true;
    parentForm.addEventListener('submit', function(e){
      if(holdSubmit){
        e.preventDefault()
        if(originalSelect.querySelectorAll('[selected]').length > 0 || originalSelect.value > ""){

          var resetSelect = false;

          if(originalSelect.value){
            if(combo.value != originalSelect.querySelector('option[value="' + originalSelect.value +'"]').text){
              resetSelect = true;
            }
          }
          if(resetSelect){
            originalSelect.value = "";
            if(originalSelect.querySelectorAll('[selected]').length > 0){
              originalSelect.querySelectorAll('[selected]')[0].removeAttribute('selected');
            }
          }
        }

        holdSubmit = false;
        //parentForm.submit();
        HTMLFormElement.prototype.submit.call(parentForm); // because submit buttons have id of "submit" which masks the form's natural form.submit() function
      }
    })

  }
  function checkForAutocompleteLoad(){
    if(parentForm.querySelector('[role="combobox"]')){
      clearInterval(checkForLoad)
      polyfillAutocomplete();
    }
  }
}

const submitButton = document.querySelector('#submit');

if(submitButton) {
  submitButton.addEventListener("click", showTheSpinner);
}

function showTheSpinner() {

    const processingWheel = document.getElementById("processing-wheel");

    //make the spinning wheel visible
    processingWheel.removeAttribute("aria-hidden");
    processingWheel.hidden = false;

    //make the elements hidden
    document.getElementById("processing-button-group").style.visibility = 'hidden';

    //make spinner visible in High Contrast Mode
    const processing = document.getElementById("processing");

    processing.innerHTML = '';
    processing.innerHTML = '<h2 class="govuk-heading-m">We are processing your return.</h2><div><svg class="processing-wheel-loader" height="100" width="100"><circle cx="50" cy="50" r="40"  fill="none"/></svg></div>';

    return false;
}

document.addEventListener("DOMContentLoaded", function() {
    const continueButton = document.getElementById("file-upload-continue");
    const radios = document.querySelectorAll('input[name="value"]');

    if (!continueButton || radios.length === 0) return;

    const status = continueButton.dataset.status;
    const hasRedirectError = continueButton.dataset.redirectError === "true";

    if (status !== "UPLOADED") {
        continueButton.setAttribute("disabled", "disabled");
    } else {
        continueButton.removeAttribute("disabled");
    }

   for (var i = 0; i < radios.length; i++)  {
        radios[i].addEventListener("change", function() {
            if (status === "UPLOADED") {
                // Always enabled if file uploaded
                continueButton.removeAttribute("disabled");
            } else if (status === "FAILED" || hasRedirectError) {
                // Only enable if user selects "false"
                if (this.value === "false") {
                    continueButton.removeAttribute("disabled");
                } else {
                    continueButton.setAttribute("disabled", "disabled");
                }
            } else {
                // UPLOADING or any other state → always disabled
                continueButton.setAttribute("disabled", "disabled");
            }
        });
    }
});

