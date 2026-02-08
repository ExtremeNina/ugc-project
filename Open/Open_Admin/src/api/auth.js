import { post } from './request.js';

/**
 * 登录API
 * @param {Object} loginData - 登录数据
 * @param {string} loginData.username - 用户名
 * @param {string} loginData.password - 密码
 * @returns {Promise} - 返回登录结果
 */
export const login = (loginData) => {
  // 使用相对路径，通过Vite代理转发到后端
  const endpoint = '/admin/login';
  
  return post(endpoint, loginData);
};

/**
 * 注册API
 * @param {Object} registerData - 注册数据
 * @param {string} registerData.username - 用户名
 * @param {string} registerData.password - 密码
 * @param {string} registerData.email - 邮箱
 * @param {string} registerData.verificationCode - 验证码
 * @returns {Promise} - 返回注册结果
 */
export const register = (registerData) => {
  // 使用相对路径，通过Vite代理转发到后端
  const endpoint = '/admin/register';
  
  return post(endpoint, registerData);
};

/**
 * 发送验证码API
 * @param {Object} emailData - 邮箱数据
 * @param {string} emailData.email - 邮箱地址
 * @returns {Promise} - 返回发送结果
 */
export const sendVerificationCode = (emailData) => {
  // 使用相对路径，通过Vite代理转发到后端
  const endpoint = '/admin/sendVerificationCode';
  
  return post(endpoint, emailData);
};