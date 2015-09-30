/*******************************************************************************
 * Copyright (c) 2015, Eclipse Foundation, Inc. and its licensors.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * - Neither the name of the Eclipse Foundation, Inc. nor the names of its
 *   contributors may be used to endorse or promote products derived from this
 *   software without specific prior written permission. 
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *******************************************************************************/
/// <reference path="template.ts" />
/// <reference path="jquery.d.ts" />
// WARNING: Do not edit the *.js version of this file. Instead, always edit the
// corresponding *.ts source in the ts subfolder, and then invoke the
// compileTypescript.sh bash script to generate new *.js and *.js.map files.
workbench.addLoad(function createFederatePageLoaded() {
    function respondToFormState() {
        var memberID = $('input.memberID');
        var enoughMembers = memberID.filter(':checked').length >= 2;
        if (enoughMembers) {
            $('#create-feedback').hide();
        }
        else {
            $('#create-feedback').show();
        }
        var fedID = $('#id').val();
        var validID = /.+/.test(fedID);
        var disable = !(validID && enoughMembers);
        var matchExisting = false;
        // test that fedID not equal any existing id
        memberID.each(function () {
            if (fedID == $(this).attr('value')) {
                disable = true;
                matchExisting = true;
                return false;
            }
        });
        var recurseMessage = $('#recurse-message');
        if (matchExisting) {
            recurseMessage.show();
        }
        else {
            recurseMessage.hide();
        }
        $('input#create').prop('disabled', disable);
    }
    /**
     * Calls another function with a delay of 0 msec. (Workaround for annoying
     * browser behavior.)
     */
    function timeoutRespond() {
        setTimeout(respondToFormState, 0);
    }
    respondToFormState();
    $('input.memberID').on('change', respondToFormState);
    $("input[name='type']").on('change', respondToFormState);
    $('#id').off().on('keydown paste cut', timeoutRespond);
});
//# sourceMappingURL=create-federate.js.map