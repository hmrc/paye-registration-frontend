/*
 * Script adapted from timeout-dialog.js v1.0.1, 01-03-2012
 *
 * @author: Rodrigo Neri (@rigoneri)
 *
 * (The MIT License)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

Date.now = Date.now || function() { return +new Date; };

function secondsToTime (secs) {
    var hours = Math.floor(secs / (60 * 60))

    var divisorForMinutes = secs % (60 * 60)
    var minutes = Math.floor(divisorForMinutes / 60)

    var divisorForSeconds = divisorForMinutes % 60
    var seconds = Math.ceil(divisorForSeconds)

    var obj = {
        'h': hours,
        'm': minutes,
        's': seconds
    }
    return obj
}

String.prototype.format = function () {
    var s = this
    var i = arguments.length

    while (i--) {
        s = s.replace(new RegExp('\\{' + i + '\\}', 'gm'), arguments[i])
    }
    return s
}

!function ($) {
    $.timeoutDialog = function (options) {
        var settings = {
            timeout: 900,
            countdown: 60,
            time: 'seconds',
            title: 'You’re about to be signed out',
            message: 'For your security, we’ll sign you out in',
            keep_alive_url: '/register-for-paye/renew-session',
            logout_url: '/register-for-paye/error/destroy-session',
            restart_on_yes: true,
            dialog_width: 340,
            close_on_escape: true,
            background_no_scroll: true,
            keep_alive_button_text: 'Resume your session',
            heading_text: 'You’ve been inactive for a while.'
        }

        $.extend(settings, options)

        var keyCodes = {
            tab: 9,
            enter: 13,
            shift: 16,
            esc: 27,
            space: 32
        }

        var TimeoutDialog = {
            init: function () {
                this.setupDialogTimer()
            },

            setupDialogTimer: function () {
                var self = this
                self.dialogOpen = false
                window.setTimeout(function () {
                    self.setupDialog()
                }, ((settings.timeout) - (settings.countdown)) * 1000)
            },

            setupDialog: function () {
                var self = this
                self.dialogOpen = true
                self.startTime = Math.round(Date.now()/1000, 0)
                self.currentMin = Math.ceil(settings.timeout / 60) //900 / 60 = 15 minutes
                self.destroyDialog()
                if (settings.background_no_scroll) {
                    $('html').addClass('noScroll')
                }
                var time = secondsToTime(settings.countdown)
                var timeout = secondsToTime(settings.timeout)
                if(time.m == 1) {
                    settings.time = ' minute'
                }
                $(
                    '<div id="timeout-dialog" class="timeout-dialog" role="dialog" tabindex="-1" aria-live="polite">'
                    + '<p id="timeout-heading">' + settings.heading_text + '</p>'
                    + '<p id="timeout-message" role="text">' + settings.message + ''
                    + '    <span id="timeout-countdown" class="countdown">' + time.s + ' ' +  settings.time + '</span>'
                    + '</p>'
                    + '<button id="timeout-keep-signin-btn" class="button button--link">' + settings.keep_alive_button_text
                    + '</button>'
                    + '</div>'
                    + '<div id="timeout-overlay" class="timeout-overlay"></div>')
                    .appendTo('body')

                // AL: disable the non-dialog page to prevent confusion for VoiceOver users
                $('#skiplink-container, body>header, #global-cookie-message, body>main, body>footer').attr('aria-hidden', 'true')

                var activeElement = document.activeElement
                var modalFocus = document.getElementById("timeout-dialog")
                var resumeButton = document.getElementById("timeout-keep-signin-btn")
                resumeButton.focus()
                self.addEvents()
                self.startCountdown(settings.countdown)

                self.handleKeyPress = function (event) {
                    if (self.dialogOpen) {
                        if (event.keyCode == keyCodes.tab) {
                            event.preventDefault()
                            resumeButton.focus()
                        }

                        if (event.keyCode == keyCodes.esc) {
                            self.keepAlive()
                            activeElement.focus()
                        }
                    }
                }

                self.closeDialog = function () {
                    if (self.dialogOpen) {
                        self.keepAlive()
                        activeElement.focus()
                    }
                }

                // AL: prevent scrolling on touch, but allow pinch zoom
                self.handleTouch = function(e){
                    var touches = e.originalEvent.touches || e.originalEvent.changedTouches
                    if ($('#timeout-dialog').length) {
                        if(touches.length == 1){
                            e.preventDefault()
                        }
                    }
                }
                // AL: add touchmove handler
                $(document).on('touchmove', self.handleTouch)
                $(document).on('keydown', self.handleKeyPress)
                $('#timeout-keep-signin-btn').on('click', self.closeDialog)
                $('#timeout-keep-signin-btn').on('keydown', function(ev) {
                    if (ev.keyCode == keyCodes.enter || ev.keyCode == keyCodes.space) self.closeDialog()
                })
            },

            destroyDialog: function () {
                if ($('#timeout-dialog').length) {
                    self.dialogOpen = false
                    $('.timeout-overlay').remove();
                    $('#timeout-dialog').remove()
                    if (settings.background_no_scroll) {
                        $('html').removeClass('noScroll')
                    }
                }
                $('#skiplink-container, body>header, #global-cookie-message, body>main, body>footer').removeAttr('aria-hidden')
            },

            // AL: moved updater to own call to allow calling from other events
            updateUI: function(counter){
                var self = this
                if (counter < 60) {
                    $('.timeout-dialog').removeAttr('aria-live')
                    $('#timeout-countdown').html(counter + " seconds")
                } else {
                    var newCounter = Math.ceil(counter / 60);
                    var minutesMessage = " minutes"
                    if(newCounter == 1) {
                        minutesMessage = " minute"
                    }
                    if(newCounter < self.currentMin){
                        self.currentMin = newCounter
                        $('#timeout-countdown').html(newCounter + minutesMessage)
                    }
                }
            },

            addEvents: function(){
                var self = this;

                // trap focus in modal (or browser chrome)
                $('a, input, textarea, button, [tabindex]').not('[tabindex="-1"]').on("focus", function (event) {
                    var modalFocus = document.getElementById("timeout-dialog")
                    if(modalFocus && self.dialogOpen){
                        if(!modalFocus.contains(event.target)) {
                            event.stopPropagation()
                            modalFocus.focus()
                        }
                    }
                });

                function handleFocus(){
                    if(self.dialogOpen){
                        // clear the countdown
                        window.clearInterval(self.countdown)
                        // calculate remaining time
                        var expiredSeconds = (Math.round(Date.now()/1000, 0)) - self.startTime;
                        var currentCounter = settings.countdown - expiredSeconds;
                        self.updateUI(currentCounter);
                        self.startCountdown(currentCounter);
                    }
                }

                // AL: handle browsers pausing timeouts/intervals by recalculating the remaining time on window focus
                // need to widen this to cover the setTimeout which triggers the dialog for browsers which pause timers on blur
                // hiding this from IE8 and it breaks the reset - to investigate further
                if (navigator.userAgent.match(/MSIE 8/) == null) {
                    //$(window).on("blur", function(){
                    $(window).off("focus", handleFocus);
                    $(window).on("focus", handleFocus);
                    //});
                }
            },

            startCountdown: function (counter) {
                var self = this
                self.countdown = window.setInterval(function () {
                    counter -= 1
                    self.updateUI(counter);
                    if (counter <= 0) {
                        self.signOut()
                    }
                }, 1000)
            },

            keepAlive: function () {
                var self = this
                this.destroyDialog()
                window.clearInterval(this.countdown)
                $.get(settings.keep_alive_url, function () {
                    if (settings.restart_on_yes) {
                        self.setupDialogTimer()
                    } else {
                        self.signOut()
                    }
                })
            },

            signOut: function () {
                var self = this
                this.destroyDialog()
                window.location = settings.logout_url
            }
        }
        if($("#signOutNavHref").length) {
            TimeoutDialog.init()
        }
    }
}(window.jQuery)
