import { getDurationParts } from '../../../utils/utils';

/**
 * Chọn gói trả phí có chi phí trung bình mỗi tháng thấp nhất trong cùng loại tiền tệ.
 * @param {Array<Object>} plans
 * @returns {Object|null}
 */
export const getBestValuePlan = (plans = []) => {
  const candidates = plans
    .map((plan) => {
      const price = Number(plan?.price);
      const durationDays = Number(plan?.durationDays);
      const { unit, count } = getDurationParts(durationDays);
      let months;

      if (unit === 'year') {
        months = count * 12;
      } else if (unit === 'month') {
        months = count;
      } else {
        months = durationDays / 30;
      }

      return {
        plan,
        pricePerMonth: price / months,
        currency: String(plan?.currency ?? '').trim().toUpperCase(),
      };
    })
    .filter(
      ({ plan, pricePerMonth }) =>
        Number(plan?.price) > 0 &&
        Number.isFinite(pricePerMonth) &&
        Number.isFinite(Number(plan?.durationDays)) &&
        Number(plan.durationDays) > 0
    );

  if (candidates.length === 0) return null;

  const comparisonCurrency = candidates[0].currency;

  return candidates
    .filter(({ currency }) => currency === comparisonCurrency)
    .reduce(
      (best, candidate) =>
        candidate.pricePerMonth < best.pricePerMonth ? candidate : best,
      candidates[0]
    ).plan;
};

/**
 * Format ngày giờ thành chuỗi dd/MM/yyyy HH:mm theo locale.
 * @param {string|Date} dateInput
 * @param {string} locale
 * @returns {string}
 */
export const formatDateTime = (dateInput, locale = 'vi-VN') => {
  if (!dateInput) return '';
  const date = new Date(dateInput);
  if (isNaN(date.getTime())) return '';

  return new Intl.DateTimeFormat(locale, {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false,
  }).format(date);
};
