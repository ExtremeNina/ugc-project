import { ElMessage } from 'element-plus';

/**
 * 通用的API请求函数
 * @param {string} url - 请求URL
 * @param {Object} options - 请求选项
 * @returns {Promise} - 返回处理后的响应数据
 */
export const request = async (url, options = {}) => {
  try {
    // 设置默认选项
    const defaultOptions = {
      headers: {
        'Content-Type': 'application/json',
        'Accept': 'application/json'
      },
      credentials: 'include' // 包含cookies，支持Spring Security的会话认证
    };
    
    // 尝试获取token并添加到请求头
    const token = localStorage.getItem('token');
    if (token) {
      defaultOptions.headers['Authorization'] = `Bearer ${token}`;
    }
    
    // 合并选项
    const mergedOptions = {
      ...defaultOptions,
      ...options,
      headers: {
        ...defaultOptions.headers,
        ...options.headers
      }
    };
    
    // 发送请求
    console.log('发送API请求:', url, mergedOptions);
    const response = await fetch(url, mergedOptions);
    
    // 检查响应状态
    if (!response.ok) {
      // 对于401未授权错误，让后端处理重定向，不在这里处理
      // 其他错误尝试获取错误信息
      const contentType = response.headers.get('content-type');
      
      if (contentType && contentType.includes('application/json')) {
        try {
          const errorData = await response.json();
          throw new Error(errorData.message || `请求失败: ${response.status}`);
        } catch (jsonError) {
          throw new Error(`请求失败: ${response.status}`);
        }
      } else {
        // 如果后端返回非JSON响应，让浏览器处理重定向或其他操作
        if (response.status === 401) {
          // 对于401错误，让浏览器自然处理，可能是重定向到登录页面
          window.location.reload();
          return Promise.reject(new Error('未授权'));
        }
        throw new Error(`请求失败: ${response.status}`);
      }
    }
    
    // 检查响应类型是否为JSON
    const contentType = response.headers.get('content-type');
    if (contentType && contentType.includes('application/json')) {
      // 尝试解析JSON
      try {
        const data = await response.json();
        console.log('API响应数据:', data);
        
        // 适配后端返回的JSON响应格式
        // 支持常见的后端响应格式：{code: 1/200, data: any, message?: string}
        if (data.code !== undefined) {
          if (data.code === 1 || data.code === 200) {
            // 成功响应，返回data字段或整个数据
            return data.data !== undefined ? data.data : data;
          } else {
            // 失败响应，显示错误信息但不抛出异常，让调用者处理
            if (data.message) {
              ElMessage.warning(data.message);
            }
            return data;
          }
        } else {
          // 直接返回数据
          return data;
        }
      } catch (jsonError) {
        // JSON解析错误，可能是后端返回的非JSON格式
        console.warn('响应不是有效的JSON格式，可能是后端自定义页面或重定向');
        // 不抛出错误，让浏览器处理
        return { success: true };
      }
    } else {
      // 如果不是JSON响应，可能是后端返回的自定义页面或重定向
      console.warn('响应不是JSON格式，可能是后端自定义页面或重定向');
      // 返回成功状态，让调用者知道请求已完成
      return { success: true };
    }
  } catch (error) {
    console.error('API请求错误:', error);
    
    // 对于HTML响应错误，不显示错误信息，避免干扰用户体验
    if (!(error instanceof SyntaxError && error.message && error.message.includes('Unexpected token <'))) {
      // 只显示非HTML解析错误的提示
      ElMessage.error(error.message || '网络请求失败');
    }
    
    throw error;
  }
};

/**
 * GET请求快捷方法
 * @param {string} url - 请求URL
 * @param {Object} params - 查询参数
 * @param {Object} options - 额外选项
 * @returns {Promise} - 返回处理后的响应数据
 */
export const get = (url, params = {}, options = {}) => {
  // 构建查询字符串
  const queryString = Object.keys(params)
    .filter(key => params[key] !== undefined && params[key] !== null && params[key] !== '')
    .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`)
    .join('&');
  
  const fullUrl = queryString ? `${url}?${queryString}` : url;
  
  return request(fullUrl, {
    ...options,
    method: 'GET'
  });
};

/**
 * POST请求快捷方法
 * @param {string} url - 请求URL
 * @param {Object} data - 请求体数据
 * @param {Object} options - 额外选项
 * @returns {Promise} - 返回处理后的响应数据
 */
export const post = (url, data = {}, options = {}) => {
  return request(url, {
    ...options,
    method: 'POST',
    body: JSON.stringify(data)
  });
};

/**
 * PUT请求快捷方法
 * @param {string} url - 请求URL
 * @param {Object} data - 请求体数据
 * @param {Object} options - 额外选项
 * @returns {Promise} - 返回处理后的响应数据
 */
export const put = (url, data = {}, options = {}) => {
  return request(url, {
    ...options,
    method: 'PUT',
    body: JSON.stringify(data)
  });
};

/**
 * DELETE请求快捷方法
 * @param {string} url - 请求URL
 * @param {Object} options - 额外选项
 * @returns {Promise} - 返回处理后的响应数据
 */
export const del = (url, options = {}) => {
  return request(url, {
    ...options,
    method: 'DELETE'
  });
};