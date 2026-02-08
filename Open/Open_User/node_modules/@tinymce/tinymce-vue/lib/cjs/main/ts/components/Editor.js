"use strict";
/**
 * Copyright (c) 2018-present, Ephox, Inc.
 *
 * This source code is licensed under the Apache 2 license found in the
 * LICENSE file in the root directory of this source tree.
 *
 */
var __assign = (this && this.__assign) || function () {
    __assign = Object.assign || function(t) {
        for (var s, i = 1, n = arguments.length; i < n; i++) {
            s = arguments[i];
            for (var p in s) if (Object.prototype.hasOwnProperty.call(s, p))
                t[p] = s[p];
        }
        return t;
    };
    return __assign.apply(this, arguments);
};
Object.defineProperty(exports, "__esModule", { value: true });
exports.Editor = void 0;
var ScriptLoader_1 = require("../ScriptLoader");
var TinyMCE_1 = require("../TinyMCE");
var Utils_1 = require("../Utils");
var EditorPropTypes_1 = require("./EditorPropTypes");
var vue_1 = require("vue");
var renderInline = function (ce, id, elementRef, tagName) {
    return ce(tagName ? tagName : 'div', {
        id: id,
        ref: elementRef
    });
};
var renderIframe = function (ce, id, elementRef) {
    return ce('textarea', {
        id: id,
        visibility: 'hidden',
        ref: elementRef
    });
};
var defaultInitValues = { selector: undefined, target: undefined };
exports.Editor = (0, vue_1.defineComponent)({
    props: EditorPropTypes_1.editorProps,
    setup: function (props, ctx) {
        var conf = props.init ? __assign(__assign({}, props.init), defaultInitValues) : __assign({}, defaultInitValues);
        var _a = (0, vue_1.toRefs)(props), disabled = _a.disabled, modelValue = _a.modelValue, tagName = _a.tagName;
        var element = (0, vue_1.ref)(null);
        var vueEditor = null;
        var elementId = props.id || (0, Utils_1.uuid)('tiny-vue');
        var inlineEditor = (props.init && props.init.inline) || props.inline;
        var modelBind = !!ctx.attrs['onUpdate:modelValue'];
        var mounting = true;
        var initialValue = props.initialValue ? props.initialValue : '';
        var cache = '';
        var getContent = function (isMounting) { return modelBind ?
            function () { return ((modelValue === null || modelValue === void 0 ? void 0 : modelValue.value) ? modelValue.value : ''); } :
            function () { return isMounting ? initialValue : cache; }; };
        var initWrapper = function () {
            var content = getContent(mounting);
            var finalInit = __assign(__assign({}, conf), { readonly: props.disabled, target: element.value, plugins: (0, Utils_1.mergePlugins)(conf.plugins, props.plugins), toolbar: props.toolbar || (conf.toolbar), inline: inlineEditor, setup: function (editor) {
                    vueEditor = editor;
                    editor.on('init', function (e) { return (0, Utils_1.initEditor)(e, props, ctx, editor, modelValue, content); });
                    if (typeof conf.setup === 'function') {
                        conf.setup(editor);
                    }
                } });
            if ((0, Utils_1.isTextarea)(element.value)) {
                element.value.style.visibility = '';
            }
            (0, TinyMCE_1.getTinymce)().init(finalInit);
            mounting = false;
        };
        (0, vue_1.watch)(disabled, function (disable) {
            var _a;
            if (vueEditor !== null) {
                if (typeof ((_a = vueEditor.mode) === null || _a === void 0 ? void 0 : _a.set) === 'function') {
                    vueEditor.mode.set(disable ? 'readonly' : 'design');
                }
                else {
                    vueEditor.setMode(disable ? 'readonly' : 'design');
                }
            }
        });
        (0, vue_1.watch)(tagName, function (_) {
            var _a;
            if (!modelBind) {
                cache = vueEditor.getContent();
            }
            (_a = (0, TinyMCE_1.getTinymce)()) === null || _a === void 0 ? void 0 : _a.remove(vueEditor);
            (0, vue_1.nextTick)(function () { return initWrapper(); });
        });
        (0, vue_1.onMounted)(function () {
            if ((0, TinyMCE_1.getTinymce)() !== null) {
                initWrapper();
            }
            else if (element.value && element.value.ownerDocument) {
                var channel = props.cloudChannel ? props.cloudChannel : '6';
                var apiKey = props.apiKey ? props.apiKey : 'no-api-key';
                var scriptSrc = (0, Utils_1.isNullOrUndefined)(props.tinymceScriptSrc) ?
                    "https://cdn.tiny.cloud/1/".concat(apiKey, "/tinymce/").concat(channel, "/tinymce.min.js") :
                    props.tinymceScriptSrc;
                ScriptLoader_1.ScriptLoader.load(element.value.ownerDocument, scriptSrc, initWrapper);
            }
        });
        (0, vue_1.onBeforeUnmount)(function () {
            if ((0, TinyMCE_1.getTinymce)() !== null) {
                (0, TinyMCE_1.getTinymce)().remove(vueEditor);
            }
        });
        if (!inlineEditor) {
            (0, vue_1.onActivated)(function () {
                if (!mounting) {
                    initWrapper();
                }
            });
            (0, vue_1.onDeactivated)(function () {
                var _a;
                if (!modelBind) {
                    cache = vueEditor.getContent();
                }
                (_a = (0, TinyMCE_1.getTinymce)()) === null || _a === void 0 ? void 0 : _a.remove(vueEditor);
            });
        }
        var rerender = function (init) {
            var _a;
            cache = vueEditor.getContent();
            (_a = (0, TinyMCE_1.getTinymce)()) === null || _a === void 0 ? void 0 : _a.remove(vueEditor);
            conf = __assign(__assign(__assign({}, conf), init), defaultInitValues);
            (0, vue_1.nextTick)(function () { return initWrapper(); });
        };
        ctx.expose({
            rerender: rerender,
            getEditor: function () { return vueEditor; }
        });
        return function () { return inlineEditor ?
            renderInline(vue_1.h, elementId, element, props.tagName) :
            renderIframe(vue_1.h, elementId, element); };
    }
});
