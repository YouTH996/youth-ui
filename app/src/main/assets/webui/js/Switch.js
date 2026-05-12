/**
 * Switch 切换
 *
 */
;
(function($, window, document, undefined) {
    'use strict';
    // 默认参数
    var Data = {
        defaults: {
            class: '', // 添加class
            left: '是', // 左边文字
            right: '否', // 右边文字
            checked: false, // 默认是否选中
            selected:'',
            option: [],
            value: ['1', '2'], // input返回的value
            icon: false, //是否显示字体图标
            dragg: false,
            id: '', // input ID值
            name: '', // input Name值
            bg: 'bg_blue', // 颜色
            disable: false, // 是否禁用
        }
    };

    // 插件引擎
    var Engine = {
        // 获取属性设置
        getAttrSettings: function($original) {
            let defaults = Data.defaults;
            var attrData = {};
            $.each(defaults, (key, val) => {
                if ($original.is('[data-' + key + ']')) {
                    var elAttr = $original.attr('data-' + key);
                    if ((key === "value" || key === "option") && elAttr !== "") { elAttr = elAttr.split(','); }
                    if (elAttr === 'true' || elAttr === 'false') { elAttr = elAttr === 'true'; }
                    if (elAttr === "") { elAttr = defaults[key]; }
                    attrData[key] = elAttr;
                }
            })
            return attrData;
        },
        // 初始化
        initialize: function($original, userSettings) {
            var self = this;
            var settings = $.extend(true, {}, userSettings);
            var attrSettings = self.getAttrSettings($original);
            settings = $.extend(settings, attrSettings);
            var $plutinHtml = Build.build($original, settings);
            $plutinHtml.insertBefore($original);
            $original.remove();
            Binds.bind($plutinHtml, settings);
            return $plutinHtml;
        },
        // 检查目标
        controlTarget: function($target, controls) {
            // 初始化时是否是input
            if ($.inArray('isInput', controls) !== -1 && !$target.is('input[type=switch]')) {
                console.error('switch | initialization failed，Invalid input element');
                console.log($target[0]);
                return false;
            }
            // 是否初始化
            if ($.inArray('isInitialized', controls) !== -1 && !$target.hasClass('switch')) {
                console.error('switch | select is not initialized');
                console.log($target[0]);
                return false;
            }
            // 是否唯一
            if ($.inArray('isSingle', controls) !== -1 && $target.length > 1) {
                console.error('switch | Can only be called on a single element');
                console.log($target[0]);
                return false;
            }
            return true;
        },
        // 检查values
        controlValues: function($target, values) {
            if (!(values instanceof Object)) {
                console.error('values parameter is not a valid Object');
                console.log($target[0]);
                console.log(values);
                return false;
            }
            return true;
        },
        // 设置value
        setVal: function($target, valObjs) {
            let self = this,
                val,
                text;
            if (valObjs.selected !== undefined) {
                var idx = $target.data('value').split(',').indexOf(valObjs.selected);
                $target.find('.switch_select_inner').removeClass("active");
                $target.find('.switch_select_inner').eq(idx).addClass("active");
            }else if (valObjs.checked !== undefined) {
                if (valObjs.checked === "true" || valObjs.checked === true) {
                    $target.removeClass('switch_bg').addClass('switch_checked');
                    val = $target.data('value').split(',')[0];
                    text = $target.data('left');
                } else {
                    $target.removeClass('switch_checked').addClass('switch_bg');
                    val = $target.data('value').split(',')[1];
                    text = $target.data('right');
                }
                $target.find('input[type=hidden]').val(val);
                $target.find('.switch_inner').text(text);
                $(document).trigger('switch:' + $target.data('class'), $target.find('input[type=hidden]').val());
            }
        },
        //保存设置
        setGlobal:function(key, val){
            key.startsWith("system_") ? window.carJsBridge.setConfig(key, parseInt(val)) : setGlobal(key, val);
        }
    }
    // 创建
    var Build = {
        // 初始化HTML
        build: function($original, settings) {
            let self = this;
            //console.log(settings);
            // 增加switch
            let $switch = $(`<div class="switch ${settings.disable === true?'switch_stop':''} ${settings.class} ${settings.checked?'switch_checked':'switch_bg'} ${settings.option.length>2?'switch_select':''} ${settings.bg}" data-class="${settings.class}" data-left="${settings.left}" data-right="${settings.right}" data-value="${settings.value.toString()}"></div>`)
            // 新增圆点
            let dot = $('<div class="switch_dot" />');
            // switch内容
            let text = settings.checked ? settings.icon == true ? '<i class="ic i_ok"></i>' : settings.left : settings.icon == true ? '<i class="ic i_close"></i>' : settings.right;
            // 当前选中值显示位置
            let textHtml = $(`<div class="switch_inner">${text}</div>`);
            
            if(settings.option.length > 2){
                let opHtml = '';
                for (let i = 0; i < settings.option.length; i++) {
                    opHtml += `<div class="switch_select_inner${settings.selected == settings.value[i]?' active':''}">${settings.option[i]}</div>`;
                };
                dot = $('<div class="switch_option_dot" />');
                textHtml = $(opHtml);
            }

            $switch.append(dot, textHtml);
            let valHtml = self.buildValue(settings);
            valHtml.insertBefore(dot);
            return $switch;
        },
        // 当前选中value
        buildValue: function(settings) {
            let value = settings.option.length>2 ? settings.selected : settings.checked ? settings.value[0] : settings.value[1];
            let $valHtml = $(`<input class="dn" type="hidden" id="${settings.id}" name="${settings.name}" value="${value}"></input>`);
            return $valHtml;
        }
    }
    // 事件
    var Binds = {
        // 初始化事件
        bind: function($plutinHtml, settings) {
            var self = this;
            if (!$plutinHtml.is('.switch_stop')) {
                $plutinHtml.off('touchstart.switch').on('touchstart.switch', { settings: settings }, self.touchstart);
                if(settings.option.length > 2){
                    $plutinHtml.off('touchstart.switch').on('touchstart', '.switch_select_inner', { settings: settings }, self.touchstart);
                }
            }
            return settings;
        },
        // 点击事件
        touchstart: function(e) {
            let t = $(this),
                isSelect = (e.currentTarget.className == "switch_select_inner"),
                settings = e.data.settings,
                elId = isSelect ? t.siblings('input[type=hidden]').attr("id") : t.find('input[type=hidden]').attr("id");
            if(settings.dragg){
                let index = elId.lastIndexOf("_"); 
                var pId = elId.replace(elId.substring(index + 1, elId .length),"position");
                if(global(pId)) $.removeCookie(pId,{ path: '/'});
            }
            if(isSelect){
                var curVal = settings.value[(t.index()-2)];
                t.addClass("active");
                t.siblings(".switch_select_inner").removeClass("active");
                t.siblings('input[type=hidden]').val(curVal);
                elId && Engine.setGlobal(elId, curVal);
            }else{
                if (t.hasClass('switch_checked')) {
                    t.removeClass('switch_checked');
                    t.addClass('switch_bg');
                    t.find('.switch_inner').html(settings.icon ? '<i class="ic i_close"></i>' : settings.right);
                    t.find('input[type=hidden]').val(settings.value[1]);
                    elId && Engine.setGlobal(elId, settings.value[1]);
                    if(elId == "system_levitate_amap_mode"){
                        localStorage.removeItem("levitate_amap_setting");
                    }
                } else {
                    t.addClass('switch_checked');
                    t.removeClass('switch_bg');
                    t.find('.switch_inner').html(settings.icon ? '<i class="ic i_ok"></i>' : settings.left);
                    t.find('input[type=hidden]').val(settings.value[0]);                
                    elId && Engine.setGlobal(elId, settings.value[0]);
                    if(elId == "system_custom_wallpaper_mode"){
                        Engine.setGlobal("carousel_wallpaper_show", 1);
                    }
                }
            }

            switch(elId){
                case "car_tire_widget_show":
                case "weather_widget_show":
                case "player_mini_lyric_show":
                    $(".widget_iframe")[0].contentWindow.updateWidgetLayout(Math.random());
                    break;
                case "player_blur_show":
                    $(".widget_iframe")[0].contentWindow.updateMusic(Math.random());
                    break;
                case "system_custom_wallpaper_mode":
                    wallpaperInit(Math.random());
                    break;
                case "bing_wallpaper_mode":
                    car.getBingWallpaper(Math.random());
                    break;
                case "car_model_show":
                    window.location.reload();
                    break;
                case "words_day_show":
                    car.getWordsDay(Math.random());
                    car.updateLayout(1);
                    break;
                default:
                    car.updateLayout(1);
                    break;
            }
            
            //(elId == "car_tire_widget_show" || elId == "player_mini_lyric_show") ? $(".widget_iframe")[0].contentWindow.updateWidgetLayout(Math.random()) : elId == "player_blur_show" ? $(".widget_iframe")[0].contentWindow.updateMusic(Math.random()) : car.updateLayout(1);
            
            $(document).trigger('switch:' + settings.class, t.find('input[type=hidden]').val());
        }
    }
    // 方法操作
    var Methods = {
        // 初始化
        init: function(opts) {
            var settings = $.extend(true, {}, Data.defaults, opts);
            var $original = $(this);
            if (Engine.controlTarget($original, ['isInput'])) {
                return Engine.initialize($original, settings);
            }

        },
        // 设置开启与关闭
        set: function(values) {
            if (Engine.controlTarget(this, ['isSingle', 'isInitialized'])) {
                if (Engine.controlValues(this, values)) {
                    Engine.setVal(this, values);
                }
            }
        }
    
    }
    // 使用插件
    $.fn.Switch = function(options) {
        if (this.length < 1) { return; }
        // 判断是什么操作
        if (Methods[options]) {
            // 传入进来的具有length属性的第一个参数arguments转换为数组，再调用它的slice（截取）方法
            var slicedArguments = Array.prototype.slice.call(arguments, 1);
            return Methods[options].apply(this, slicedArguments);
        } else if (typeof options === 'object' || !options) {
            return Methods.init.apply(this, arguments);
        } else {
            console.error('Switch | Call error');
            console.log(this);
        }
    };

})(jQuery, window, document);