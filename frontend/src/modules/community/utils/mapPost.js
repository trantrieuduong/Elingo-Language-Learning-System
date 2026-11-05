export const mapPost = (post, groups = []) => ({
  ...post,
  author: {
    id: post.author?.id,
    fullName: post.author?.fullName,
    username: post.author?.username,
    avatarUrl: post.author?.avatarUrl,
  },
  medias:
    post.medias?.map((media) => ({
      type: media.mediaType?.toLowerCase(),
      url: media.mediaUrl,
    })) || [],
  groupName: groups.find((group) => group.id === post.groupId)?.groupName || '',
});
